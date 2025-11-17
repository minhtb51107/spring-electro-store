package com.minh.springelectrostore.product.repository;

import com.minh.springelectrostore.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
    // Spring Data JPA sẽ tự hiểu: "tìm một Brand dựa trên trường 'slug'"
    Optional<Brand> findBySlug(String slug);
    
    // Tương tự, tìm dựa trên 'name'
    Optional<Brand> findByName(String name);
}