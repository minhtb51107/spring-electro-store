package com.minh.springelectrostore.modules.product.worker;

import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
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

    /**
     * Chạy bất đồng bộ: Main thread trả về ngay lập tức,
     * thread pool sẽ xử lý việc trừ DB sau.
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Luôn chạy transaction mới
    public void syncDecreaseStock(Long variantId, Integer quantity) {
        try {
            // Logic trừ kho DB cũ (vẫn giữ điều kiện >= quantity để an toàn lớp cuối)
            int updated = productVariantRepository.decreaseStock(variantId, quantity);
            
            if (updated == 0) {
                // Đây là trường hợp Data Inconsistency (Redis còn, DB hết)
                // Cần log lại để Admin xử lý thủ công hoặc chạy job đối soát
                log.error("CRITICAL: Lệch tồn kho! Redis cho phép bán nhưng DB thất bại. VariantId: {}", variantId);
            } else {
                log.info("[Async-DB] Đồng bộ trừ kho thành công VariantId: {}", variantId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ kho xuống DB: {}", e.getMessage());
            // TODO: Đẩy vào Queue "Dead Letter" để retry sau
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncIncreaseStock(Long variantId, Integer quantity) {
        try {
            productVariantRepository.increaseStock(variantId, quantity);
            log.info("[Async-DB] Đồng bộ hoàn kho thành công VariantId: {}", variantId);
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ hoàn kho: {}", e.getMessage());
        }
    }
}