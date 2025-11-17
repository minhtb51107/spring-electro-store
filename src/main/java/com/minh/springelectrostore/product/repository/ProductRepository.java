package com.minh.springelectrostore.product.repository;

import com.minh.springelectrostore.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <-- Kỹ thuật "Pro"
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                           JpaSpecificationExecutor<Product> { // <-- Kế thừa interface này

    Optional<Product> findBySlug(String slug);

    // Dùng để kiểm tra ràng buộc khi xóa Brand/Category (Module 1)
    boolean existsByBrandId(Long brandId);
    boolean existsByCategoryId(Long categoryId);
}