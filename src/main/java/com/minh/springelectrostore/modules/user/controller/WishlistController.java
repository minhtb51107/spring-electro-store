package com.minh.springelectrostore.modules.user.controller;

import com.minh.springelectrostore.common.annotation.CurrentUser;
import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.modules.user.entity.User;
import com.minh.springelectrostore.modules.user.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    public ResponseEntity<String> toggleWishlist(@CurrentUser User user, @PathVariable Long productId) {
        wishlistService.toggleWishlist(user.getEmail(), productId);
        return ResponseEntity.ok("Cập nhật danh sách yêu thích thành công.");
    }

    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> getMyWishlist(
            @CurrentUser User user,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(wishlistService.getMyWishlist(user.getEmail(), pageable));
    }
    
    @GetMapping("/{productId}/check")
    public ResponseEntity<Boolean> checkWishlist(@CurrentUser User user, @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.isProductInWishlist(user.getEmail(), productId));
    }
}