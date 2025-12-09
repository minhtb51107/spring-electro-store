package com.minh.springelectrostore.modules.search.service;

import com.minh.springelectrostore.modules.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.modules.search.event.ProductSyncEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchService {
    void handleProductSyncEvent(ProductSyncEvent event);
    void deleteProductFromIndex(Long productId);
    void indexProduct(Long productId);

    // [CẬP NHẬT] Hàm search trả về Page, hỗ trợ phân trang
    Page<ProductSummaryResponse> searchProducts(String keyword, ProductSearchCriteria criteria, Pageable pageable);
}