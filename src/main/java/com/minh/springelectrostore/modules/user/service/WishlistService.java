package com.minh.springelectrostore.modules.user.service;

import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {
    void toggleWishlist(String email, Long productId); // Thêm nếu chưa có, Xóa nếu có rồi
    Page<ProductSummaryResponse> getMyWishlist(String email, Pageable pageable);
    boolean isProductInWishlist(String email, Long productId);
}