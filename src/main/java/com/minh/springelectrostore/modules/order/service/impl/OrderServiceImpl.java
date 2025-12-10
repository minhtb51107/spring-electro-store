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
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        log.info("Bắt đầu quy trình tạo đơn hàng cho user: {}", userEmail);

        // 1. Validate dữ liệu đầu vào
        CartResponse cart = cartService.getCart(userEmail);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang rỗng.");
        }
        
        Customer customer = customerRepository.findByUser_Email(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng."));

        // 2. Tính toán giá tiền (Tạm tính) & Validate sản phẩm tồn tại
        BigDecimal tempTotalPrice = BigDecimal.ZERO; // Đổi tên biến tạm để tránh nhầm lẫn
        List<CartItemResponse> validatedItems = new ArrayList<>();

        for (CartItemResponse item : cart.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getProductVariantId())
                    .orElseThrow(() -> new BadRequestException("Sản phẩm " + item.getProductName() + " không còn tồn tại."));
            
            item.setPrice(variant.getPrice()); 
            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            tempTotalPrice = tempTotalPrice.add(lineTotal); // Biến này thay đổi liên tục
            validatedItems.add(item);
        }

        // 3. Tính toán các chi phí khác bên ngoài transaction
        // Tạo các biến FINAL (hoặc effectively final) để dùng trong lambda
        final BigDecimal verifiedTotalPrice = tempTotalPrice; // [FIX LỖI TẠI ĐÂY] Gán giá trị chốt vào biến final
        
        BigDecimal discountAmount = calculateDiscount(request.getVoucherCode(), verifiedTotalPrice, userEmail);
        AddressData addressData = validateAndGetAddress(request, customer);
        BigDecimal shippingFee = calculateShippingFee(cart, addressData, verifiedTotalPrice);

        // Tính tổng cuối cùng
        BigDecimal finalPrice = verifiedTotalPrice.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        // 4. TRANSACTION & COMPENSATION
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        
        List<CartItemResponse> reservedItems = new ArrayList<>();
        AtomicReference<OrderResponse> responseRef = new AtomicReference<>();

        try {
            Order savedOrder = transactionTemplate.execute(status -> {
                try {
                    // A. Lưu Order vào DB
                    Order order = new Order();
                    order.setCustomer(customer);
                    order.setCustomerName(addressData.finalName);
                    order.setShippingAddress(addressData.finalAddress);
                    order.setShippingPhone(addressData.finalPhone);
                    order.setNotes(request.getNotes());
                    order.setStatus(OrderStatus.PENDING);
                    order.setVoucherCode(request.getVoucherCode());
                    
                    // [FIX] Bây giờ dùng verifiedTotalPrice ở đây sẽ không bị lỗi nữa
                    order.setTotalPrice(verifiedTotalPrice); 
                    order.setDiscountAmount(discountAmount);
                    order.setShippingFee(shippingFee);
                    order.setFinalPrice(finalPrice);

                    Set<OrderItem> orderItems = new HashSet<>();
                    
                    // B. Xử lý từng item: Trừ kho Redis & Tạo OrderItem
                    for (CartItemResponse cartItem : validatedItems) {
                        Long variantId = cartItem.getProductVariantId();
                        
                        inventoryService.reserveStock(variantId, cartItem.getQuantity());
                        
                        reservedItems.add(cartItem);

                        OrderItem item = new OrderItem();
                        item.setOrder(order);
                        item.setQuantity(cartItem.getQuantity());
                        item.setPriceAtPurchase(cartItem.getPrice());
                        item.setProductVariant(productVariantRepository.getReferenceById(variantId));
                        orderItems.add(item);
                    }
                    order.setItems(orderItems);

                    // C. Save Order
                    Order result = orderRepository.save(order);

                    // D. Apply Voucher
                    if (StringUtils.hasText(request.getVoucherCode())) {
                        voucherService.applyVoucher(request.getVoucherCode(), userEmail, result.getId());
                    }

                    // E. Xóa giỏ hàng
                    cartService.clearCart(userEmail);
                    
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
            
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn hàng: {}. Đang thực hiện hoàn kho...", e.getMessage());
            for (CartItemResponse item : reservedItems) {
                try {
                    inventoryService.restoreStock(item.getProductVariantId(), item.getQuantity());
                } catch (Exception ex) {
                    log.error("Lỗi nghiêm trọng: Không thể hoàn kho cho Variant {} - Cần xử lý thủ công!", item.getProductVariantId());
                }
            }
            throw e;
        }

        return responseRef.get();
    }

    // --- Helper Methods (Đã tách ra cho gọn code chính) ---

    private BigDecimal calculateDiscount(String voucherCode, BigDecimal total, String email) {
        if (StringUtils.hasText(voucherCode)) {
            return voucherService.calculateDiscount(voucherCode, total, email);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateShippingFee(CartResponse cart, AddressData addressData, BigDecimal total) {
        try {
            int totalWeight = cart.getItems().stream().mapToInt(CartItemResponse::getQuantity).sum() * 500;
            Integer distId = addressData.addressObj.getGhnDistrictId();
            String wardCode = addressData.addressObj.getGhnWardCode();
            
            if (distId != null && wardCode != null) {
                return shippingService.calculateShippingFee(distId, wardCode, totalWeight, total.intValue());
            }
        } catch (Exception e) {
            log.warn("Lỗi tính phí ship: {}", e.getMessage());
        }
        return BigDecimal.valueOf(30000); // Default fee
    }

    private static class AddressData {
        Address addressObj;
        String finalName;
        String finalPhone;
        String finalAddress;
    }

    private AddressData validateAndGetAddress(CheckoutRequest request, Customer customer) {
        AddressData data = new AddressData();

        // Logic cũ của bạn khá ổn, tôi chỉ clean up lại một chút
        if (request.getAddressId() != null) {
            data.addressObj = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại."));
            if (!data.addressObj.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không hợp lệ.");
            }
        } else {
            // Validate sơ bộ
            if (!StringUtils.hasText(request.getNewReceiverName()) || 
                !StringUtils.hasText(request.getNewReceiverPhone()) || 
                !StringUtils.hasText(request.getNewStreetAddress())) {
                
                // Fallback lấy từ Customer profile nếu thiếu
                 if (!StringUtils.hasText(request.getCustomerName()) || !StringUtils.hasText(request.getShippingAddress())) {
                     throw new BadRequestException("Vui lòng điền đầy đủ thông tin giao hàng.");
                 }
            }
            
            Address newAddress = new Address();
            newAddress.setCustomer(customer);
            newAddress.setReceiverName(StringUtils.hasText(request.getNewReceiverName()) ? request.getNewReceiverName() : request.getCustomerName());
            newAddress.setReceiverPhone(StringUtils.hasText(request.getNewReceiverPhone()) ? request.getNewReceiverPhone() : request.getShippingPhone());
            newAddress.setStreetAddress(StringUtils.hasText(request.getNewStreetAddress()) ? request.getNewStreetAddress() : request.getShippingAddress());
            
            newAddress.setGhnProvinceId(request.getNewProvinceId());
            newAddress.setGhnDistrictId(request.getNewDistrictId());
            newAddress.setGhnWardCode(request.getNewWardCode());

            newAddress.setProvince(StringUtils.hasText(request.getNewProvinceName()) ? request.getNewProvinceName() : "Khác");
            newAddress.setDistrict(StringUtils.hasText(request.getNewDistrictName()) ? request.getNewDistrictName() : "Khác");
            newAddress.setWard(StringUtils.hasText(request.getNewWardName()) ? request.getNewWardName() : "Khác");
            newAddress.setDefault(false);
            
            data.addressObj = addressRepository.save(newAddress);
        }
        
        data.finalName = data.addressObj.getReceiverName();
        data.finalPhone = data.addressObj.getReceiverPhone();
        data.finalAddress = String.format("%s, %s, %s, %s", 
            data.addressObj.getStreetAddress(), 
            data.addressObj.getWard(), 
            data.addressObj.getDistrict(), 
            data.addressObj.getProvince());
            
        return data;
    }

    // --- Các method khác (cancel, getDetail) giữ nguyên như cũ ---
    @Override
    @Transactional
    public OrderResponse cancelMyOrder(String userEmail, Long orderId) {
        Customer customer = customerRepository.findByUser_Email(userEmail).orElseThrow();
        Order order = orderRepository.findByIdAndCustomerIdWithItems(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng khi đang chờ xử lý.");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        // Hoàn kho cho từng item
        for (OrderItem item : order.getItems()) {
            inventoryService.restoreStock(item.getProductVariant().getId(), item.getQuantity());
        }
        voucherService.refundVoucher(order.getId());
        
        Order savedOrder = orderRepository.save(order);
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