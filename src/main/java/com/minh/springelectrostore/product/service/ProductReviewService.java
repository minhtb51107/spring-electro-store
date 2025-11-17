package com.minh.springelectrostore.product.service;

import com.minh.springelectrostore.product.dto.request.CreateReviewRequest;
import com.minh.springelectrostore.product.dto.request.UpdateReviewRequest;
import com.minh.springelectrostore.product.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {
    ReviewResponse createReview(String userEmail, CreateReviewRequest request);
    
    Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
    
    ReviewResponse updateReview(String userEmail, Long reviewId, UpdateReviewRequest request);
    
    void deleteReview(String userEmail, Long reviewId);
}