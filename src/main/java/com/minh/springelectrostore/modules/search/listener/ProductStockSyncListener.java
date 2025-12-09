package com.minh.springelectrostore.modules.search.listener;

import com.minh.springelectrostore.modules.order.event.OrderPlacedEvent;
import com.minh.springelectrostore.modules.search.worker.ProductSyncWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStockSyncListener {

    private final ProductSyncWorker productSyncWorker;

    // Chỉ chạy khi Transaction DB đã Commit thành công (Order đã lưu, Tồn kho DB đã trừ)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Nhận sự kiện OrderPlacedEvent cho Order #{}", event.getOrder().getId());

        // Lấy danh sách Product ID (distinct) từ các items trong đơn hàng
        Set<Long> productIds = event.getOrder().getItems().stream()
                .map(item -> item.getProductVariant().getProduct().getId())
                .collect(Collectors.toSet());

        // Kích hoạt worker
        productSyncWorker.syncProductsToElasticsearch(event.getOrder().getId(), productIds);
    }
}