package com.minh.springelectrostore.product.controller;

import com.minh.springelectrostore.product.dto.request.CreateReviewRequest;
import com.minh.springelectrostore.product.dto.request.UpdateReviewRequest;
import com.minh.springelectrostore.product.dto.response.ReviewResponse;
import com.minh.springelectrostore.product.service.impl.ProductReviewServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ProductReviewServiceImpl reviewService;

    // API Công khai: Xem review của sản phẩm
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable("productId") Long productId, // <--- THÊM ("productId")
            @PageableDefault(size = 5) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    // API Bảo mật: Viết review (Chỉ user đã mua hàng)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request) {
        
        ReviewResponse response = reviewService.createReview(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable("id") Long id, // <--- THÊM ("id")
            Authentication authentication,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Service sẽ check quyền Owner hoặc Admin
    public ResponseEntity<Void> deleteReview(
            @PathVariable("id") Long id, // <--- THÊM ("id")
            Authentication authentication) {
        reviewService.deleteReview(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}