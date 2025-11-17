package com.minh.springelectrostore.order.service.impl;

import com.minh.springelectrostore.cart.dto.response.CartItemResponse;
import com.minh.springelectrostore.cart.dto.response.CartResponse;
import com.minh.springelectrostore.cart.service.CartService;
import com.minh.springelectrostore.order.dto.request.CheckoutRequest;
import com.minh.springelectrostore.order.dto.response.OrderResponse;
import com.minh.springelectrostore.order.dto.response.OrderSummaryResponse;
import com.minh.springelectrostore.order.entity.Order;
import com.minh.springelectrostore.order.entity.OrderItem;
import com.minh.springelectrostore.order.entity.OrderStatus;
import com.minh.springelectrostore.order.mapper.OrderMapper;
import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.order.service.OrderService;
import com.minh.springelectrostore.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.promotion.service.VoucherService; // Import
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.service.EmailService;
import com.minh.springelectrostore.shipping.service.ShippingService;
import com.minh.springelectrostore.user.entity.Address;
import com.minh.springelectrostore.user.entity.Customer;
import com.minh.springelectrostore.user.repository.AddressRepository;
import com.minh.springelectrostore.user.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final VoucherService voucherService; // Inject VoucherService
    private final AddressRepository addressRepository;
    private final ShippingService shippingService;

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

        // --- 3. LOGIC XỬ LÝ ĐỊA CHỈ (NÂNG CẤP) ---
        String finalName;
        String finalAddress;
        String finalPhone;

        if (request.getAddressId() != null) {
            // Case A: User chọn từ sổ địa chỉ
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại."));
            
            // Security Check: Địa chỉ này có phải của user này không?
            if (!address.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không hợp lệ (không thuộc về bạn).");
            }

            finalName = address.getReceiverName();
            finalPhone = address.getReceiverPhone();
            // Format địa chỉ đầy đủ
            finalAddress = address.getStreetAddress() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getProvince();
        } else {
            // Case B: User nhập tay (Validate thủ công vì bỏ @NotBlank ở DTO)
            if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
                throw new BadRequestException("Tên người nhận không được để trống.");
            }
            if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
                throw new BadRequestException("Địa chỉ giao hàng không được để trống.");
            }
            if (request.getShippingPhone() == null || request.getShippingPhone().trim().isEmpty()) {
                throw new BadRequestException("Số điện thoại không được để trống.");
            }

            finalName = request.getCustomerName();
            finalAddress = request.getShippingAddress();
            finalPhone = request.getShippingPhone();
        }
        // -----------------------------------------

        // 4. Khởi tạo đơn hàng
        Order order = new Order();
        order.setCustomer(customer);
        order.setCustomerName(finalName);
        order.setShippingAddress(finalAddress);
        order.setShippingPhone(finalPhone);
        order.setNotes(request.getNotes());
        order.setStatus(OrderStatus.PENDING);

        // 5. Logic Tính tiền & Voucher
        BigDecimal totalPrice = cart.getTotalPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            discountAmount = voucherService.calculateDiscount(
                request.getVoucherCode(), 
                totalPrice, 
                userEmail
            );
            order.setVoucherCode(request.getVoucherCode());
        }
        
        BigDecimal finalPrice = totalPrice.subtract(discountAmount).max(BigDecimal.ZERO);

        order.setTotalPrice(totalPrice);
        order.setDiscountAmount(discountAmount);
        order.setFinalPrice(finalPrice);

        // 6. Xử lý Order Items và Trừ kho
        Set<OrderItem> orderItems = new HashSet<>();
        for (CartItemResponse cartItem : cart.getItems()) {
            int updatedRows = productVariantRepository.decreaseStock(
                cartItem.getProductVariantId(),
                cartItem.getQuantity()
            );

            if (updatedRows == 0) {
                throw new BadRequestException("Sản phẩm '" + cartItem.getProductName() + "' đã hết hàng hoặc không đủ số lượng.");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtPurchase(cartItem.getPrice());
            item.setProductVariant(
                productVariantRepository.getReferenceById(cartItem.getProductVariantId())
            );
            
            orderItems.add(item);
        }
        order.setItems(orderItems);

        // 7. Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);
        
        // 8. Áp dụng Voucher chính thức
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            voucherService.applyVoucher(request.getVoucherCode(), userEmail, savedOrder.getId());
        }

        log.info("Đã tạo đơn hàng ID: {} | Voucher: {} | Giảm: {}", 
                 savedOrder.getId(), request.getVoucherCode(), discountAmount);

        // 9. Dọn dẹp & Thông báo
        cartService.clearCart(userEmail);
        
        String emailBody = String.format(
            "<h1>Cảm ơn bạn đã đặt hàng!</h1>" +
            "<p>Đơn hàng #%d của bạn đã được tiếp nhận.</p>" +
            "<p>Tổng tiền hàng: %,.0f VND</p>" +
            "<p>Giảm giá: -%,.0f VND</p>" +
            "<p><strong>Thành tiền: %,.0f VND</strong></p>",
            savedOrder.getId(),
            savedOrder.getTotalPrice(),
            savedOrder.getDiscountAmount() != null ? savedOrder.getDiscountAmount() : BigDecimal.ZERO,
            savedOrder.getFinalPrice()
        );
        emailService.sendEmail(userEmail, "Xác nhận đơn hàng #" + savedOrder.getId(), emailBody);
        
        Address address = addressRepository.findById(request.getAddressId()).orElseThrow();
        
     // TÍNH PHÍ SHIP
        BigDecimal shippingFee = shippingService.calculateShippingFee(
            address.getGhnDistrictId(),
            address.getGhnWardCode(),
            cart.getTotalItems() * 500, // Giả sử mỗi món 500g
            cart.getTotalPrice().intValue()
        );
        
        order.setShippingFee(shippingFee);
        
        // TÍNH TỔNG TIỀN CUỐI CÙNG
        // finalPrice = (cartTotal + shippingFee) - discount
        BigDecimal finalPrice1 = cart.getTotalPrice().add(shippingFee).subtract(discountAmount);
        order.setFinalPrice(finalPrice1);

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
        
        // --- HOÀN LẠI VOUCHER ---
        voucherService.refundVoucher(savedOrder.getId());
        // ------------------------

        log.warn("Đơn hàng ID: {} đã hủy.", savedOrder.getId());
        return orderMapper.toOrderResponse(savedOrder);
    }
    
    // ... (Các phương thức getMyOrders, getMyOrderDetail giữ nguyên) ...
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