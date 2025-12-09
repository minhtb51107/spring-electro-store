package com.minh.springelectrostore.modules.search.worker;

import com.minh.springelectrostore.modules.search.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSyncWorker {

    private final ProductSyncService productSyncService;

    // Async để chạy ngầm, không block luồng chính
    @Async("taskExecutor") 
    public void syncProductsToElasticsearch(Long orderId, Set<Long> productIds) {
        log.info("[Async-Worker] Bắt đầu đồng bộ {} sản phẩm cho Order #{}", productIds.size(), orderId);
        
        for (Long productId : productIds) {
            try {
                // Gọi hàm indexProduct (Full re-index) để cập nhật cả tồn kho và soldQuantity
                productSyncService.indexProduct(productId);
            } catch (Exception e) {
                log.error("[Async-Worker] Lỗi đồng bộ Product ID: {} - {}", productId, e.getMessage());
            }
        }
    }
}