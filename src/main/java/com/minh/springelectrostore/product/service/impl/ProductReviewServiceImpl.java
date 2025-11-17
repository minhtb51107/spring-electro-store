package com.minh.springelectrostore.product.service.impl;

import com.minh.springelectrostore.order.repository.OrderRepository;
import com.minh.springelectrostore.product.dto.request.CreateReviewRequest;
import com.minh.springelectrostore.product.dto.request.UpdateReviewRequest;
import com.minh.springelectrostore.product.dto.response.ReviewResponse;
import com.minh.springelectrostore.product.entity.Product;
import com.minh.springelectrostore.product.entity.ProductReview;
import com.minh.springelectrostore.product.mapper.ReviewMapper;
import com.minh.springelectrostore.product.repository.ProductRepository;
import com.minh.springelectrostore.product.repository.ProductReviewRepository;
import com.minh.springelectrostore.product.service.ProductReviewService;
import com.minh.springelectrostore.search.event.ProductSyncEvent; // Import Event
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher; // Import Publisher
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher eventPublisher; // Inject để bắn sự kiện sync

    @Override
    @Transactional
    public ReviewResponse createReview(String userEmail, CreateReviewRequest request) {
        // 1. Check Verified Purchase (Đã mua hàng chưa?)
        boolean hasPurchased = orderRepository.hasUserPurchasedProduct(userEmail, request.getProductId());
        if (!hasPurchased) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng thành công.");
        }
        
        // 2. Check Duplicate (Chống Spam: 1 user - 1 review/sản phẩm)
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), request.getProductId())) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 3. Build Entity
        ProductReview review = ProductReview.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        
        // Xử lý ảnh
        if (request.getImageUrls() != null) {
            request.getImageUrls().forEach(review::addImage);
        }

        ProductReview savedReview = reviewRepository.save(review);

        // 4. Cập nhật điểm đánh giá trung bình cho Product
        updateProductRating(product);

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String userEmail, Long reviewId, UpdateReviewRequest request) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check quyền sở hữu
        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Bạn không có quyền sửa đánh giá này.");
        }

        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getComment() != null) review.setComment(request.getComment());
        
        // Cập nhật ảnh
        if (request.getImageUrls() != null) {
            review.getImages().clear();
            request.getImageUrls().forEach(review::addImage);
        }

        ProductReview savedReview = reviewRepository.save(review);

        // Cập nhật điểm đánh giá trung bình (vì rating có thể thay đổi)
        updateProductRating(review.getProduct());

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional
    public void deleteReview(String userEmail, Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User user = userRepository.findByEmail(userEmail).orElseThrow();

        // Check quyền: Chủ sở hữu HOẶC Admin
        boolean isOwner = review.getUser().getEmail().equals(userEmail);
        boolean isAdmin = user.getEmployee() != null; 

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này.");
        }

        Product product = review.getProduct(); // Lưu lại product để update sau khi xóa review
        reviewRepository.delete(review);

        // Cập nhật lại điểm đánh giá trung bình
        updateProductRating(product);
    }
    
    /**
     * Hàm helper quan trọng:
     * 1. Tính toán lại số sao trung bình và tổng số review.
     * 2. Lưu vào bảng Product (PostgreSQL).
     * 3. Bắn sự kiện để đồng bộ sang Elasticsearch (để bộ lọc sort theo rating hoạt động đúng).
     */
    private void updateProductRating(Product product) {
        List<ProductReview> reviews = reviewRepository.findAllByProductId(product.getId());
        
        if (reviews.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double avg = reviews.stream()
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);
            
            // Làm tròn 1 chữ số thập phân (ví dụ 4.5)
            product.setAverageRating(Math.round(avg * 10.0) / 10.0);
            product.setReviewCount(reviews.size());
        }
        
        productRepository.save(product);
        
        // Đồng bộ sang Elasticsearch
        eventPublisher.publishEvent(new ProductSyncEvent(
            this, 
            product.getId(), 
            ProductSyncEvent.SyncAction.CREATE_UPDATE
        ));
    }
}