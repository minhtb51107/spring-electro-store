package com.minh.springelectrostore.cart.service;

import com.minh.springelectrostore.cart.dto.request.CartItemRequest;
import com.minh.springelectrostore.cart.dto.request.UpdateCartItemRequest;
import com.minh.springelectrostore.cart.dto.response.CartResponse;

public interface CartService {

    /**
     * Lấy giỏ hàng hiện tại của người dùng (từ Redis).
     * @param userEmail Email của người dùng đã đăng nhập (dùng làm key).
     * @return CartResponse (kể cả giỏ hàng rỗng).
     */
    CartResponse getCart(String userEmail);

    /**
     * Thêm một sản phẩm vào giỏ hàng hoặc cập nhật số lượng nếu đã tồn tại.
     * @param userEmail Email của người dùng.
     * @param request DTO chứa productVariantId và quantity.
     * @return Giỏ hàng sau khi đã cập nhật.
     */
    CartResponse addItemToCart(String userEmail, CartItemRequest request);

    /**
     * Cập nhật số lượng của một món hàng đã có trong giỏ.
     * @param userEmail Email của người dùng.
     * @param productVariantId ID của biến thể sản phẩm cần cập nhật.
     * @param request DTO chỉ chứa số lượng mới.
     * @return Giỏ hàng sau khi đã cập nhật.
     */
    CartResponse updateItemQuantity(String userEmail, Long productVariantId, UpdateCartItemRequest request);

    /**
     * Xóa một món hàng khỏi giỏ.
     * @param userEmail Email của người dùng.
     * @param productVariantId ID của biến thể sản phẩm cần xóa.
     * @return Giỏ hàng sau khi đã cập nhật.
     */
    CartResponse removeItemFromCart(String userEmail, Long productVariantId);

    /**
     * Xóa sạch toàn bộ giỏ hàng (thường dùng sau khi checkout thành công).
     * @param userEmail Email của người dùng.
     */
    void clearCart(String userEmail);
}