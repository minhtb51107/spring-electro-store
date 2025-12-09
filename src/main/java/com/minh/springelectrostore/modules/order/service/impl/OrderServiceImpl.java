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
import org.springframework.util.StringUtils; // [Import Mới] Dùng để check text

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

        CartResponse cart = cartService.getCart(userEmail);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang rỗng.");
        }
        
        Customer customer = customerRepository.findByUser_Email(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng."));

        AddressData addressData = validateAndGetAddress(request, customer);

        BigDecimal verifiedTotalPrice = BigDecimal.ZERO;
        
        for (CartItemResponse item : cart.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getProductVariantId())
                    .orElseThrow(() -> new BadRequestException("Sản phẩm " + item.getProductName() + " không còn tồn tại."));
            
            item.setPrice(variant.getPrice()); 
            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            verifiedTotalPrice = verifiedTotalPrice.add(lineTotal);
        }

        BigDecimal tempDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            tempDiscount = voucherService.calculateDiscount(request.getVoucherCode(), verifiedTotalPrice, userEmail);
        }
        BigDecimal discountAmount = tempDiscount;

        BigDecimal shippingFee;
        try {
            int totalWeight = cart.getItems().stream().mapToInt(CartItemResponse::getQuantity).sum() * 500;
            
            Integer distId = addressData.addressObj.getGhnDistrictId();
            String wardCode = addressData.addressObj.getGhnWardCode();
            
            if (distId == null || wardCode == null) {
                log.warn("Địa chỉ thiếu thông tin GHN, sử dụng phí ship mặc định.");
                shippingFee = BigDecimal.valueOf(30000); 
            } else {
                shippingFee = shippingService.calculateShippingFee(
                    distId,
                    wardCode,
                    totalWeight,
                    verifiedTotalPrice.intValue()
                );
            }
        } catch (Exception e) {
            log.error("Lỗi tính phí ship (sẽ dùng default): {}", e.getMessage());
            shippingFee = BigDecimal.valueOf(30000);
        }

        BigDecimal finalPrice = verifiedTotalPrice.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicReference<OrderResponse> responseRef = new AtomicReference<>();
        
        BigDecimal finalVerifiedTotal = verifiedTotalPrice;
        BigDecimal finalShippingFee = shippingFee;
        
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
                
                order.setTotalPrice(finalVerifiedTotal);
                order.setDiscountAmount(discountAmount);
                order.setShippingFee(finalShippingFee);
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
    
    private static class AddressData {
        Address addressObj;
        String finalName;
        String finalPhone;
        String finalAddress;
    }

    private AddressData validateAndGetAddress(CheckoutRequest request, Customer customer) {
        AddressData data = new AddressData();

        // CASE 1: Sử dụng địa chỉ có sẵn
        if (request.getAddressId() != null) {
            data.addressObj = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại."));
            if (!data.addressObj.getCustomer().getId().equals(customer.getId())) {
                throw new BadRequestException("Địa chỉ không hợp lệ.");
            }
            data.finalName = data.addressObj.getReceiverName();
            data.finalPhone = data.addressObj.getReceiverPhone();
            data.finalAddress = data.addressObj.getStreetAddress() + ", " + 
                                data.addressObj.getWard() + ", " + 
                                data.addressObj.getDistrict() + ", " + 
                                data.addressObj.getProvince();
        } 
        // CASE 2: Nhập địa chỉ mới
        else {
            String recName = request.getNewReceiverName() != null ? request.getNewReceiverName() : request.getCustomerName();
            String recPhone = request.getNewReceiverPhone() != null ? request.getNewReceiverPhone() : request.getShippingPhone();
            String streetAddr = request.getNewStreetAddress() != null ? request.getNewStreetAddress() : request.getShippingAddress();

            if (recName == null || recPhone == null || streetAddr == null) {
                throw new BadRequestException("Vui lòng điền đầy đủ tên, số điện thoại và địa chỉ nhận hàng.");
            }

            Address newAddress = new Address();
            newAddress.setCustomer(customer);
            newAddress.setReceiverName(recName);
            newAddress.setReceiverPhone(recPhone);
            
            // Map GHN fields (ID có thể null)
            newAddress.setGhnProvinceId(request.getNewProvinceId());
            newAddress.setGhnDistrictId(request.getNewDistrictId());
            newAddress.setGhnWardCode(request.getNewWardCode());

            // [FIX CRITICAL] Đặt giá trị mặc định "Khác" nếu không có tên
            // Điều này giúp vượt qua validation @NotBlank của Entity Address
            newAddress.setProvince(StringUtils.hasText(request.getNewProvinceName()) ? request.getNewProvinceName() : "Khác");
            newAddress.setDistrict(StringUtils.hasText(request.getNewDistrictName()) ? request.getNewDistrictName() : "Khác");
            newAddress.setWard(StringUtils.hasText(request.getNewWardName()) ? request.getNewWardName() : "Khác");
            
            newAddress.setStreetAddress(streetAddr);
            newAddress.setDefault(false);

            data.addressObj = addressRepository.save(newAddress);

            data.finalName = newAddress.getReceiverName();
            data.finalPhone = newAddress.getReceiverPhone();
            
            // Format địa chỉ hiển thị
            String fullAddr = newAddress.getStreetAddress();
            // Chỉ nối chuỗi nếu không phải là giá trị mặc định "Khác"
            if (!"Khác".equals(newAddress.getWard())) fullAddr += ", " + newAddress.getWard();
            if (!"Khác".equals(newAddress.getDistrict())) fullAddr += ", " + newAddress.getDistrict();
            if (!"Khác".equals(newAddress.getProvince())) fullAddr += ", " + newAddress.getProvince();
            
            data.finalAddress = fullAddr;
        }
        return data;
    }

    // ... (Các phương thức khác giữ nguyên) ...
    @Override
    @Transactional
    public OrderResponse cancelMyOrder(String userEmail, Long orderId) {
        Customer customer = customerRepository.findByUser_Email(userEmail).orElseThrow();
        Order order = orderRepository.findByIdAndCustomerIdWithItems(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng khi đang chờ xử lý (PENDING).");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
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