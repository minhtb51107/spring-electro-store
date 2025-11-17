package com.minh.springelectrostore.order.dto.request;

import com.minh.springelectrostore.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private OrderStatus newStatus; // Ví dụ: SHIPPED, PROCESSING, CANCELLED
}