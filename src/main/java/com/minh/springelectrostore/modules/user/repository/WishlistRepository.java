package com.minh.springelectrostore.modules.user.repository;

import com.minh.springelectrostore.modules.user.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    // Tìm wishlist của user, sắp xếp mới nhất lên đầu
    Page<Wishlist> findByUser_EmailOrderByAddedAtDesc(String email, Pageable pageable);

    // Kiểm tra user đã thích sản phẩm này chưa
    boolean existsByUser_EmailAndProduct_Id(String email, Long productId);

    // Tìm để xóa
    Optional<Wishlist> findByUser_EmailAndProduct_Id(String email, Long productId);
}