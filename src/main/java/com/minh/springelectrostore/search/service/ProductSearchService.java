package com.minh.springelectrostore.search.service;

import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchService {

    /**
     * Tìm kiếm sản phẩm bằng Elasticsearch, kết hợp full-text search và filtering.
     *
     * @param keyword  Từ khóa tìm kiếm (ví dụ: "laptop dell", "ipohne")
     * @param criteria Các bộ lọc (ví dụ: category, brand, price)
     * @param pageable Phân trang
     * @return Trang (Page) các sản phẩm tóm tắt
     */
    Page<ProductSummaryResponse> searchProducts(String keyword, ProductSearchCriteria criteria, Pageable pageable);
}