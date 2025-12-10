package com.minh.springelectrostore.modules.user.service.impl;

import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.modules.product.entity.Product;
import com.minh.springelectrostore.modules.product.mapper.ProductMapper;
import com.minh.springelectrostore.modules.product.repository.ProductRepository;
import com.minh.springelectrostore.modules.user.entity.User;
import com.minh.springelectrostore.modules.user.entity.Wishlist;
import com.minh.springelectrostore.modules.user.repository.UserRepository;
import com.minh.springelectrostore.modules.user.repository.WishlistRepository;
import com.minh.springelectrostore.modules.user.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper; // Sử dụng mapper có sẵn để convert Product -> Response

    @Override
    @Transactional
    public void toggleWishlist(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Optional<Wishlist> existing = wishlistRepository.findByUser_EmailAndProduct_Id(email, productId);

        if (existing.isPresent()) {
            // Nếu đã có -> Xóa (Unlike)
            wishlistRepository.delete(existing.get());
        } else {
            // Nếu chưa có -> Thêm (Like)
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getMyWishlist(String email, Pageable pageable) {
        // Lấy danh sách Wishlist và map sang ProductSummaryResponse
        return wishlistRepository.findByUser_EmailOrderByAddedAtDesc(email, pageable)
                .map(wishlist -> productMapper.toSummaryResponse(wishlist.getProduct()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInWishlist(String email, Long productId) {
        return wishlistRepository.existsByUser_EmailAndProduct_Id(email, productId);
    }
}