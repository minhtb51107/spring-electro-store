package com.minh.springelectrostore.search.repository;

import com.minh.springelectrostore.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * Spring Data Elasticsearch sẽ tự động hiểu phương thức này
     * và tạo ra một câu query "bool" (boolean query)
     * để tìm kiếm trên nhiều trường (name, description, categoryName, brandName).
     *
     * @param name Từ khóa cho tên
     * @param description Từ khóa cho mô tả
     * @param category Từ khóa cho danh mục
     * @param brand Từ khóa cho thương hiệu
     * @param pageable Phân trang
     * @return Trang (Page) các sản phẩm
     */
    Page<ProductDocument> findByNameOrDescriptionOrCategoryNameOrBrandName(
            String name, String description, String category, String brand, Pageable pageable
    );

    // Kỹ thuật "Pro" hơn (dùng sau): Viết query JSON của Elasticsearch
    // @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description\", \"categoryName\", \"brandName\"], \"fuzziness\": \"AUTO\"}}")
    // Page<ProductDocument> searchFuzzy(String keyword, Pageable pageable);
}