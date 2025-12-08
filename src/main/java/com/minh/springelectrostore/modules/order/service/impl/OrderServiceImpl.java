package com.minh.springelectrostore.modules.order.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.modules.cart.dto.response.CartItemResponse;
import com.minh.springelectrostore.modules.cart.dto.response.CartResponse;
import com.minh.springelectrostore.modules.cart.service.CartService;
import com.minh.springelectrostore.modules.order.dto.request.CheckoutRequest;
import com.minh.springelectrostore.modules.order.dto.response.OrderResponse;
import com.minh.springelectrostore.modules.order.dto.response.OrderSummaryResponse;
import com.minh.springelectrostore.modules.order.entity.Order;
import com.minh.springelectrostore.modules.order.entity.OrderItem;
import com.minh.springelectrostore.modules.order.entity.OrderStatus;
import com.minh.springelectrostore.modules.order.event.OrderPlacedEvent;
import com.minh.springelectrostore.modules.order.mapper.OrderMapper;
import com.minh.springelectrostore.modules.order.repository.OrderRepository;
import com.minh.springelectrostore.modules.order.service.OrderService;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.promotion.service.VoucherService;
import com.minh.springelectrostore.modules.shipping.service.ShippingService;
import com.minh.springelectrostore.modules.user.entity.Address;
import com.minh.springelectrostore.modules.user.entity.Customer;
import com.minh.springelectrostore.modules.user.repository.AddressRepository;
import com.minh.springelectrostore.modules.user.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final VoucherService voucherService;
    private final AddressRepository addressRepository;
    private final ShippingService shippingService;
    
    // Inject Event Publisher và Redisson
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient; 

    @Override
    @Transactional
    public OrderResponse createOrderFromCart(String userEmail, CheckoutRequest request) {
        
        // 1. Lấy giỏ hàng
        CartResponse cart = cartService.getCart(userEmail);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang rỗng. Không thể đặt hàng.");
        }
        
        // 2. Lấy thông tin khách hàng
        Customer customer = customerRepository.findByUser_Email(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng."));

        // 3. Xử lý địa chỉ
        String finalName, finalAddress, finalPhone;
        if (request.getAddressId() != null) {
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại."));
            if (!address.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không hợp lệ (không thuộc về bạn).");
            }
            finalName = address.getReceiverName();
            finalPhone = address.getReceiverPhone();
            finalAddress = address.getStreetAddress() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getProvince();
        } else {
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) throw new BadRequestException("Tên người nhận trống.");
            if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) throw new BadRequestException("Địa chỉ giao hàng trống.");
            if (request.getShippingPhone() == null || request.getShippingPhone().trim().isEmpty()) throw new BadRequestException("Số điện thoại trống.");
            finalName = request.getCustomerName();
            finalAddress = request.getShippingAddress();
            finalPhone = request.getShippingPhone();
        }

        // 4. Khởi tạo đơn hàng
        Order order = new Order();
        order.setCustomer(customer);
        order.setCustomerName(finalName);
        order.setShippingAddress(finalAddress);
        order.setShippingPhone(finalPhone);
        order.setNotes(request.getNotes());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalPrice = cart.getTotalPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            discountAmount = voucherService.calculateDiscount(request.getVoucherCode(), totalPrice, userEmail);
            order.setVoucherCode(request.getVoucherCode());
        }
        
        BigDecimal finalPriceTemp = totalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
        order.setTotalPrice(totalPrice);
        order.setDiscountAmount(discountAmount);
        order.setFinalPrice(finalPriceTemp);

        // --- 5. LOGIC TRỪ KHO VỚI REDISSON LOCK (Quan trọng) ---
        Set<OrderItem> orderItems = new HashSet<>();
        for (CartItemResponse cartItem : cart.getItems()) {
            Long variantId = cartItem.getProductVariantId();
            String lockKey = "lock:product_variant:" + variantId;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // Cố gắng lấy khóa: Đợi tối đa 5s, giữ khóa tối đa 10s
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new BadRequestException("Hệ thống đang bận xử lý sản phẩm '" + cartItem.getProductName() + "', vui lòng thử lại!");
                }

                // VÀO VÙNG AN TOÀN (CRITICAL SECTION)
                int updatedRows = productVariantRepository.decreaseStock(variantId, cartItem.getQuantity());
                if (updatedRows == 0) {
                    throw new BadRequestException("Sản phẩm '" + cartItem.getProductName() + "' đã hết hàng hoặc không đủ số lượng.");
                }

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setQuantity(cartItem.getQuantity());
                item.setPriceAtPurchase(cartItem.getPrice());
                item.setProductVariant(productVariantRepository.getReferenceById(variantId));
                orderItems.add(item);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Lỗi hệ thống khi xử lý đồng bộ kho.");
            } finally {
                // Giải phóng khóa
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        order.setItems(orderItems);

        // 6. Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);
        
        // 7. Áp dụng Voucher
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            voucherService.applyVoucher(request.getVoucherCode(), userEmail, savedOrder.getId());
        }

        // 8. Tính phí ship & Cập nhật giá cuối
        Address address = addressRepository.findById(request.getAddressId()).orElseThrow();
        BigDecimal shippingFee = shippingService.calculateShippingFee(
            address.getGhnDistrictId(),
            address.getGhnWardCode(),
            cart.getTotalItems() * 500, 
            cart.getTotalPrice().intValue()
        );
        savedOrder.setShippingFee(shippingFee);
        
        BigDecimal finalPrice = totalPrice.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);
        savedOrder.setFinalPrice(finalPrice);
        savedOrder = orderRepository.save(savedOrder);

        // 9. Dọn dẹp & Bắn sự kiện
        cartService.clearCart(userEmail);
        log.info("Bắn sự kiện OrderPlacedEvent cho đơn hàng {}", savedOrder.getId());
        eventPublisher.publishEvent(new OrderPlacedEvent(this, savedOrder));

        return orderMapper.toOrderResponse(savedOrder);
    }
    
    @Override
    @Transactional
    public OrderResponse cancelMyOrder(String userEmail, Long orderId) {
        Customer customer = customerRepository.findByUser_Email(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Thông tin khách hàng lỗi."));

        Order order = orderRepository.findByIdAndCustomerIdWithItems(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Không thể hủy đơn hàng.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        
        voucherService.refundVoucher(savedOrder.getId());

        log.warn("Đơn hàng ID: {} đã hủy.", savedOrder.getId());
        return orderMapper.toOrderResponse(savedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(String userEmail, Pageable pageable) {
        Customer customer = customerRepository.findByUser_Email(userEmail).orElseThrow();
        return orderRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(orderMapper::toOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderDetail(String userEmail, Long orderId) {
        Customer customer = customerRepository.findByUser_Email(userEmail).orElseThrow();
        Order order = orderRepository.findByIdAndCustomerIdWithItems(orderId, customer.getId()).orElseThrow();
        return orderMapper.toOrderResponse(order);
    }
}