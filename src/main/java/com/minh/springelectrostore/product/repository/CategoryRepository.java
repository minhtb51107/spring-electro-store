package com.minh.springelectrostore.product.repository;

import com.minh.springelectrostore.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    // Truy vấn này dùng để lấy TẤT CẢ các danh mục GỐC (không có cha)
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findAllRootCategories();

    // Truy vấn này dùng để lấy các danh mục con trực tiếp của một danh mục cha
    List<Category> findByParent_Id(Long parentId);
}