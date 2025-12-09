package com.minh.springelectrostore.modules.order.entity;

public enum OrderStatus {
    PENDING,        // Mới tạo, chờ xử lý
    PROCESSING,     // Đang xử lý (đã xác nhận, đang đóng gói)
    SHIPPED,        // Đã giao cho đơn vị vận chuyển
    DELIVERED,      // Đã giao thành công
    CANCELLED,      // Đã hủy (bởi khách hoặc admin)
    RETURNED,       // Đã trả hàng
    SHIPPING, 
    PAID
}