package com.minh.springelectrostore.modules.search.repository;

import com.minh.springelectrostore.modules.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    
    // Tìm theo tên hoặc mô tả
    List<ProductDocument> findByNameOrDescription(String name, String description);
    
    // [FIX] Đổi tên hàm thành số ít (Slug) để khớp với field 'categorySlug' trong Document
    List<ProductDocument> findByCategorySlugIn(List<String> categorySlugs);
}