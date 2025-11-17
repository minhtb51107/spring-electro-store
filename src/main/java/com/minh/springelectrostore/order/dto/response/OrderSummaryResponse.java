package com.minh.springelectrostore.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO này chỉ chứa thông tin tóm tắt.
 * Dùng cho trang "Lịch sử đơn hàng" (tránh tải quá nhiều dữ liệu).
 */
@Data
@Builder
public class OrderSummaryResponse {
    private Long id;
    private String status;
    private OffsetDateTime createdAt;
    private BigDecimal totalPrice;
    private int totalItems;
}