package com.minh.springelectrostore.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <-- Kỹ thuật "Pro"
import org.springframework.stereotype.Repository;

import com.minh.springelectrostore.modules.product.entity.Product;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                           JpaSpecificationExecutor<Product> { // <-- Kế thừa interface này

    Optional<Product> findBySlug(String slug);

    // Dùng để kiểm tra ràng buộc khi xóa Brand/Category (Module 1)
    boolean existsByBrandId(Long brandId);
    boolean existsByCategoryId(Long categoryId);
}