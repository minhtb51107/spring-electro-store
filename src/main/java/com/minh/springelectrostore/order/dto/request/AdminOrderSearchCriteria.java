package com.minh.springelectrostore.order.dto.request;

import com.minh.springelectrostore.order.entity.OrderStatus;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * DTO này dùng làm @ModelAttribute để Admin lọc đơn hàng
 * Ví dụ: /api/v1/admin/orders?status=PENDING&customerEmail=user@example.com
 */
@Data
public class AdminOrderSearchCriteria {
    
    private OrderStatus status; // Lọc theo trạng thái
    private String customerEmail; // Lọc theo email khách hàng
    private Long orderId; // Tìm theo ID đơn hàng
    
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime fromDate;
    
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime toDate;
}