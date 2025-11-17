package com.minh.springelectrostore.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * POJO này đại diện cho MỘT món hàng trong giỏ hàng.
 * Nó sẽ được lưu trong một danh sách bên trong CartResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    
    // Thông tin về sản phẩm
    private Long productVariantId;
    private String sku;
    private String productName;
    private String productSlug;
    private String thumbnailUrl;
    private BigDecimal price; // Giá của 1 sản phẩm

    // Thông tin về giỏ hàng
    private int quantity;
    private BigDecimal lineTotal; // Tổng tiền (price * quantity)
}