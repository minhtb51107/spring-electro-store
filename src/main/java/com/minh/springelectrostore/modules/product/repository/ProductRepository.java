package com.minh.springelectrostore.modules.product.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 
import org.springframework.stereotype.Repository;

import com.minh.springelectrostore.modules.product.entity.Product;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                           JpaSpecificationExecutor<Product> { 

    /**
     * Tìm sản phẩm theo Slug và load luôn Variants + Images + Category + Brand
     * attributePaths liệt kê các field cần JOIN FETCH
     */
    @EntityGraph(attributePaths = {"variants", "variants.images", "category", "brand"})
    Optional<Product> findBySlug(String slug);

    // Dùng để kiểm tra ràng buộc khi xóa Brand/Category
    boolean existsByBrandId(Long brandId);
    boolean existsByCategoryId(Long categoryId);
}