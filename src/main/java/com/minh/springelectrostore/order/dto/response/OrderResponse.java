package com.minh.springelectrostore.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO này chứa thông tin chi tiết của TOÀN BỘ đơn hàng.
 * Dùng để trả về sau khi checkout thành công hoặc khi xem chi tiết đơn hàng.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    
    private Long id;
    private String status;
    private OffsetDateTime createdAt;
    
    // Thông tin người nhận
    private String customerName;
    private String shippingAddress;
    private String shippingPhone;
    private String notes;

    // Thông tin thanh toán
    private BigDecimal totalPrice;
    private int totalItems;

    // Các món hàng
    private Set<OrderItemResponse> items;
}