package com.minh.springelectrostore.cart.controller;

import com.minh.springelectrostore.cart.dto.request.CartItemRequest;
import com.minh.springelectrostore.cart.dto.request.UpdateCartItemRequest;
import com.minh.springelectrostore.cart.dto.response.CartResponse;
import com.minh.springelectrostore.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // <-- Import quan trọng
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // <-- BẮT BUỘC: Mọi API trong đây đều phải đăng nhập
public class CartController {

    private final CartService cartService;

    /**
     * Lấy giỏ hàng của người dùng đang đăng nhập.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(Authentication authentication) {
        // Lấy email (username) từ Spring Security
        String userEmail = authentication.getName();
        CartResponse cart = cartService.getCart(userEmail);
        return ResponseEntity.ok(cart);
    }

    /**
     * Thêm một sản phẩm vào giỏ hàng.
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToMyCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request) {
        
        String userEmail = authentication.getName();
        CartResponse updatedCart = cartService.addItemToCart(userEmail, request);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Cập nhật số lượng của một món hàng trong giỏ.
     * Dùng @PathVariable để lấy ID biến thể từ URL.
     */
    @PutMapping("/items/{productVariantId}")
    public ResponseEntity<CartResponse> updateMyCartItem(
            Authentication authentication,
            @PathVariable Long productVariantId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        String userEmail = authentication.getName();
        CartResponse updatedCart = cartService.updateItemQuantity(userEmail, productVariantId, request);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Xóa một món hàng khỏi giỏ.
     */
    @DeleteMapping("/items/{productVariantId}")
    public ResponseEntity<CartResponse> removeItemFromMyCart(
            Authentication authentication,
            @PathVariable Long productVariantId) {
        
        String userEmail = authentication.getName();
        CartResponse updatedCart = cartService.removeItemFromCart(userEmail, productVariantId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Xóa sạch toàn bộ giỏ hàng.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearMyCart(Authentication authentication) {
        String userEmail = authentication.getName();
        cartService.clearCart(userEmail);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content
    }
}