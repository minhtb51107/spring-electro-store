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

    @Async("taskExecutor")
    public void syncProductsToElasticsearch(Long orderId, Set<Long> productIds) {
        log.info("[Async-Worker] Bắt đầu đồng bộ {} sản phẩm cho Order #{}", productIds.size(), orderId);
        
        try {
            for (Long productId : productIds) {
                // Gọi service logic nghiệp vụ
                productSyncService.indexProduct(productId);
            }
            log.info("[Async-Worker] Hoàn tất đồng bộ Search cho Order #{}", orderId);
        } catch (Exception e) {
            log.error("[Async-Worker] LỖI đồng bộ Search cho Order #{}", orderId, e);
        }
    }
}