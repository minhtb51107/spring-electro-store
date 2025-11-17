package com.minh.springelectrostore.search.service.impl;

import com.minh.springelectrostore.product.entity.Product;
import com.minh.springelectrostore.product.repository.ProductRepository;
import com.minh.springelectrostore.search.document.ProductDocument;
import com.minh.springelectrostore.search.event.ProductSyncEvent;
import com.minh.springelectrostore.search.mapper.ProductSyncMapper;
import com.minh.springelectrostore.search.repository.ProductSearchRepository;
import com.minh.springelectrostore.search.service.ProductSyncService;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncServiceImpl implements ProductSyncService {

    private final ProductRepository productRepository; // (1) Đọc từ Postgres
    private final ProductSearchRepository productSearchRepository; // (2) Ghi vào Elasticsearch
    private final ProductSyncMapper productSyncMapper; // (3) "Thợ dịch"

    /**
     * HÀM QUAN TRỌNG NHẤT (Câu chuyện phỏng vấn)
     * Lắng nghe sự kiện đồng bộ
     */
    @Override
    @Async("taskExecutor") // (4) Kỹ thuật "Pro": Chạy bất đồng bộ
    // (5) Kỹ thuật "Pro": Chỉ chạy khi GIAO DỊCH (ở ProductService) ĐÃ COMMIT
    @TransactionalEventListener 
    public void handleProductSyncEvent(ProductSyncEvent event) {
        log.info("Nhận được sự kiện đồng bộ cho Product ID: {} - Hành động: {}", 
                 event.getProductId(), event.getAction());
        
        try {
            switch (event.getAction()) {
                case CREATE_UPDATE:
                    // Gọi hàm index (tạo/cập nhật)
                    indexProduct(event.getProductId());
                    break;
                case DELETE:
                    // Gọi hàm delete
                    deleteProductFromIndex(event.getProductId());
                    break;
            }
        } catch (Exception e) {
            // Nếu đồng bộ lỗi (ví dụ: Elasticsearch sập), chúng ta log lại
            // Trong dự án thực tế, đây là lúc để đẩy vào "Retry Queue" (hàng đợi thử lại)
            log.error("Đồng bộ Elasticsearch thất bại cho Product ID: {}. Lỗi: {}", 
                      event.getProductId(), e.getMessage());
        }
    }

    /**
     * Đọc từ Postgres, chuyển đổi, và Ghi vào Elasticsearch
     */
    @Override
    @Transactional(readOnly = true) // Cần transaction để đọc Lazy-loading (variants, images)
    public void indexProduct(Long productId) {
        // 1. Đọc dữ liệu mới nhất từ CSDL chính (PostgreSQL)
        // (Phải dùng JOIN FETCH để lấy đủ dữ liệu cho Mapper)
        Product product = productRepository.findById(productId) 
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm " + productId + " để đồng bộ."));

        // 2. Kiểm tra nếu sản phẩm không active (bị xóa mềm)
        if (!product.isActive()) {
            deleteProductFromIndex(productId);
            log.info("Sản phẩm ID: {} không active, đã xóa khỏi index.", productId);
            return;
        }

        // 3. Dùng "thợ dịch" (Mapper) để phi chuẩn hóa
        ProductDocument document = productSyncMapper.toDocument(product);

        // 4. Ghi (hoặc cập nhật) vào Elasticsearch
        productSearchRepository.save(document);
        log.info("Đã đồng bộ (index) thành công Product ID: {} vào Elasticsearch.", document.getId());
    }

    /**
     * Xóa khỏi Elasticsearch
     */
    @Override
    public void deleteProductFromIndex(Long productId) {
        if (productSearchRepository.existsById(productId)) {
            productSearchRepository.deleteById(productId);
            log.info("Đã xóa Product ID: {} khỏi Elasticsearch index.", productId);
        }
    }
}