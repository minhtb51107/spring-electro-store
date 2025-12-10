package com.minh.springelectrostore.modules.product.worker;

import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.search.service.ProductSyncService; // <--- Import thêm
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySyncWorker {

    private final ProductVariantRepository productVariantRepository;
    private final ProductSyncService productSyncService; // <--- Inject thêm Service này

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncDecreaseStock(Long variantId, Integer quantity) {
        try {
            productVariantRepository.findById(variantId).ifPresent(variant -> {
                variant.setStockQuantity(variant.getStockQuantity() - quantity);
                productVariantRepository.save(variant);
                log.info("ASYNC DB: Đã trừ kho Variant {} đi {}.", variantId, quantity);
                
                // Lưu ý: Việc sync ES khi trừ kho đã được ProductStockSyncListener xử lý qua Event OrderPlaced.
                // Nên ở đây có thể KHÔNG cần gọi sync ES để tránh duplicate action, 
                // trừ khi bạn muốn chắc chắn 100% (double-check).
            });
        } catch (Exception e) {
            log.error("CRITICAL: Lỗi đồng bộ trừ kho xuống DB", e);
        }
    }

    /**
     * Đồng bộ hoàn kho (khi hủy đơn)
     * ĐÂY LÀ CHỖ CẦN SỬA
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncIncreaseStock(Long variantId, Integer quantity) {
        try {
            productVariantRepository.findById(variantId).ifPresent(variant -> {
                // 1. Update SQL
                variant.setStockQuantity(variant.getStockQuantity() + quantity);
                productVariantRepository.save(variant);
                log.info("ASYNC DB: Đã hoàn kho Variant {} thêm {}.", variantId, quantity);

                // 2. [THÊM MỚI] Trigger update Elasticsearch ngay lập tức
                // Vì Hủy đơn thường không bắn ra Event OrderPlaced, nên phải gọi thủ công ở đây
                Long productId = variant.getProduct().getId();
                try {
                    productSyncService.indexProduct(productId);
                    log.info("SYNC ES: Đã cập nhật lại tồn kho trên Search cho Product {}", productId);
                } catch (Exception ex) {
                    log.error("SYNC ES FAIL: Lỗi cập nhật search index khi hoàn kho", ex);
                }
            });
        } catch (Exception e) {
            log.error("CRITICAL: Lỗi đồng bộ hoàn kho xuống DB", e);
        }
    }
}