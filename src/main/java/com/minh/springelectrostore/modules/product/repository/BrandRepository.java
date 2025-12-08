package com.minh.springelectrostore.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minh.springelectrostore.modules.product.entity.Brand;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
    // Spring Data JPA sẽ tự hiểu: "tìm một Brand dựa trên trường 'slug'"
    Optional<Brand> findBySlug(String slug);
    
    // Tương tự, tìm dựa trên 'name'
    Optional<Brand> findByName(String name);
}