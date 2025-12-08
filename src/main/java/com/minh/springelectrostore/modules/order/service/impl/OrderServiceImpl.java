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
import com.minh.springelectrostore.modules.product.service.InventoryService;
import com.minh.springelectrostore.modules.promotion.service.VoucherService;
import com.minh.springelectrostore.modules.shipping.service.ShippingService;
import com.minh.springelectrostore.modules.user.entity.Address;
import com.minh.springelectrostore.modules.user.entity.Customer;
import com.minh.springelectrostore.modules.user.repository.AddressRepository;
import com.minh.springelectrostore.modules.user.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
    private final ApplicationEventPublisher eventPublisher;
    
    private final InventoryService inventoryService; 
    private final PlatformTransactionManager transactionManager;

    @Override
    public OrderResponse createOrderFromCart(String userEmail, CheckoutRequest request) {
        log.info("Bắt đầu tạo đơn hàng cho user: {}", userEmail);

        // 1. Lấy dữ liệu
        CartResponse cart = cartService.getCart(userEmail);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang rỗng.");
        }
        
        Customer customer = customerRepository.findByUser_Email(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng."));

        AddressData addressData = validateAndGetAddress(request, customer);

        // 2. Tính toán
        BigDecimal totalPrice = cart.getTotalPrice();
        
        // [FIX] Sử dụng biến tạm (tempDiscount) để tính toán
        BigDecimal tempDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            tempDiscount = voucherService.calculateDiscount(request.getVoucherCode(), totalPrice, userEmail);
        }
        // Gán giá trị cuối cùng vào biến này -> Nó trở thành "effectively final" và dùng được trong Lambda
        BigDecimal discountAmount = tempDiscount;

        BigDecimal shippingFee;
        try {
            log.info("Đang gọi GHN API để tính phí ship...");
            shippingFee = shippingService.calculateShippingFee(
                addressData.addressObj.getGhnDistrictId(),
                addressData.addressObj.getGhnWardCode(),
                cart.getTotalItems() * 500, 
                cart.getTotalPrice().intValue()
            );
            log.info("Phí ship GHN: {}", shippingFee);
        } catch (Exception e) {
            log.error("Lỗi NGHIÊM TRỌNG khi gọi GHN: {}", e.getMessage());
            throw new BadRequestException("Không thể tính phí vận chuyển: " + e.getMessage());
        }

        BigDecimal finalPrice = totalPrice.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        // 3. Mở Transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicReference<OrderResponse> responseRef = new AtomicReference<>();
        
        Order savedOrder = transactionTemplate.execute(status -> {
            try {
                Order order = new Order();
                order.setCustomer(customer);
                order.setCustomerName(addressData.finalName);
                order.setShippingAddress(addressData.finalAddress);
                order.setShippingPhone(addressData.finalPhone);
                order.setNotes(request.getNotes());
                order.setStatus(OrderStatus.PENDING);
                order.setVoucherCode(request.getVoucherCode());
                order.setTotalPrice(totalPrice);
                
                // Bây giờ discountAmount đã hợp lệ để sử dụng ở đây
                order.setDiscountAmount(discountAmount);
                
                order.setShippingFee(shippingFee);
                order.setFinalPrice(finalPrice);

                Set<OrderItem> orderItems = new HashSet<>();
                for (CartItemResponse cartItem : cart.getItems()) {
                    Long variantId = cartItem.getProductVariantId();
                    inventoryService.reserveStock(variantId, cartItem.getQuantity());

                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setQuantity(cartItem.getQuantity());
                    item.setPriceAtPurchase(cartItem.getPrice());
                    item.setProductVariant(productVariantRepository.getReferenceById(variantId));
                    orderItems.add(item);
                }
                order.setItems(orderItems);

                Order result = orderRepository.save(order);

                if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                    voucherService.applyVoucher(request.getVoucherCode(), userEmail, result.getId());
                }

                cartService.clearCart(userEmail);
                
                // Map DTO bên trong Transaction
                responseRef.set(orderMapper.toOrderResponse(result));
                
                return result;

            } catch (Exception e) {
                status.setRollbackOnly();
                throw e; 
            }
        });

        if (savedOrder != null) {
            eventPublisher.publishEvent(new OrderPlacedEvent(this, savedOrder));
        }

        return responseRef.get();
    }
    
    // --- Helper Methods ---
    private static class AddressData {
        Address addressObj;
        String finalName;
        String finalPhone;
        String finalAddress;
    }

    private AddressData validateAndGetAddress(CheckoutRequest request, Customer customer) {
        AddressData data = new AddressData();
        if (request.getAddressId() != null) {
            data.addressObj = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại."));
            if (!data.addressObj.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không hợp lệ.");
            }
            data.finalName = data.addressObj.getReceiverName();
            data.finalPhone = data.addressObj.getReceiverPhone();
            data.finalAddress = data.addressObj.getStreetAddress() + ", " + data.addressObj.getWard() + ", " + data.addressObj.getDistrict() + ", " + data.addressObj.getProvince();
        } else {
            throw new BadRequestException("Vui lòng chọn địa chỉ từ sổ địa chỉ.");
        }
        return data;
    }
    
    @Override
    @Transactional
    public OrderResponse cancelMyOrder(String userEmail, Long orderId) {
        Customer customer = customerRepository.findByUser_Email(userEmail).orElseThrow();
        Order order = orderRepository.findByIdAndCustomerIdWithItems(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Không thể hủy đơn hàng.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        voucherService.refundVoucher(savedOrder.getId());
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