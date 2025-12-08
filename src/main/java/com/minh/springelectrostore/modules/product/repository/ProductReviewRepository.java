package com.minh.springelectrostore.modules.product.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minh.springelectrostore.modules.product.entity.ProductReview;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    
    // Lấy danh sách review của 1 sản phẩm
    Page<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    
    // Kiểm tra xem user đã review sản phẩm này chưa (nếu muốn giới hạn 1 review/sản phẩm)
    boolean existsByUserIdAndProductId(Integer userId, Long productId);

	List<ProductReview> findAllByProductId(Long id);
}