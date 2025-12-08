package com.minh.springelectrostore.modules.search.listener;

import com.minh.springelectrostore.modules.order.entity.OrderItem;
import com.minh.springelectrostore.modules.order.event.OrderPlacedEvent;
import com.minh.springelectrostore.modules.search.worker.ProductSyncWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStockSyncListener {

    private final ProductSyncWorker productSyncWorker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockUpdate(OrderPlacedEvent event) {
        log.info("Listener nhận sự kiện Order #{} -> Chuyển giao cho ProductSyncWorker", event.getOrder().getId());

        // 1. Trích xuất danh sách ID sản phẩm cần sync
        Set<Long> productIds = new HashSet<>();
        if (event.getOrder().getItems() != null) {
            for (OrderItem item : event.getOrder().getItems()) {
                productIds.add(item.getProductVariant().getProduct().getId());
            }
        }

        // 2. Giao việc cho Worker chạy ngầm
        if (!productIds.isEmpty()) {
            productSyncWorker.syncProductsToElasticsearch(event.getOrder().getId(), productIds);
        }
    }
}