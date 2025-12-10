package com.minh.springelectrostore.modules.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse implements Serializable {
    
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Builder.Default
    private Integer totalItems = 0;

    // [MỚI] Thời điểm cập nhật cuối cùng để tính thời gian "ngủ"
    private Instant lastUpdatedAt;

    // [MỚI] Đánh dấu đã gửi mail nhắc nhở chưa để tránh spam
    @Builder.Default
    private boolean reminderSent = false;
}