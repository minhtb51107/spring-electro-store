package com.minh.springelectrostore.search.service;

import com.minh.springelectrostore.search.event.ProductSyncEvent;

public interface ProductSyncService {

    /**
     * Lắng nghe sự kiện thay đổi sản phẩm từ PostgreSQL
     * và đồng bộ (tạo/cập nhật) vào Elasticsearch.
     * * @param event Sự kiện chứa ID sản phẩm và hành động
     */
    void handleProductSyncEvent(ProductSyncEvent event);

    /**
     * Xử lý việc xóa một document khỏi Elasticsearch.
     *
     * @param productId ID của sản phẩm cần xóa
     */
    void deleteProductFromIndex(Long productId);

    /**
     * Xử lý việc tạo/cập nhật một document trong Elasticsearch.
     *
     * @param productId ID của sản phẩm cần đồng bộ
     */
    void indexProduct(Long productId);
}