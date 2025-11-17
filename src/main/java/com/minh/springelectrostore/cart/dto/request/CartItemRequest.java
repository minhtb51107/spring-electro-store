package com.minh.springelectrostore.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull(message = "ID biến thể sản phẩm (variantId) là bắt buộc")
    private Long productVariantId;

    @NotNull(message = "Số lượng là bắt buộc")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    private int quantity;
}