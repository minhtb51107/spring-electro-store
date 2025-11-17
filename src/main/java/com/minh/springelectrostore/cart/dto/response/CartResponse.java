package com.minh.springelectrostore.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * POJO này đại diện cho TOÀN BỘ giỏ hàng của user.
 * Đây là đối tượng sẽ được serialize thành JSON và lưu vào Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Builder.Default
    private int totalItems = 0;
}