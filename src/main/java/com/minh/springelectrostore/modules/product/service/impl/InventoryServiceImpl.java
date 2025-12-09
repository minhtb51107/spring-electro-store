package com.minh.springelectrostore.modules.product.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.product.service.InventoryService;
import com.minh.springelectrostore.modules.product.worker.InventorySyncWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional; // Bỏ Transactional ở cấp này để tăng tốc

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryRedisService inventoryRedisService;
    private final InventorySyncWorker inventorySyncWorker;

    @Override
    // Không dùng @Transactional ở đây nữa vì Redis không tham gia transaction DB
    public void reserveStock(Long variantId, Integer quantity) {
        log.info("Bắt đầu giữ hàng (High Concurrency) cho Variant: {}", variantId);

        // 1. [LAZY LOAD] Nếu Redis chưa có key (lần đầu chạy hoặc bị xóa), nạp từ DB lên
        if (!inventoryRedisService.hasStockKey(variantId)) {
            log.info("Cache Miss: Nạp tồn kho từ DB lên Redis cho Variant {}", variantId);
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại"));
            inventoryRedisService.setStock(variantId, variant.getStockQuantity());
        }

        // 2. [REDIS GATEKEEPER] Trừ kho trên Redis trước
        boolean success = inventoryRedisService.decrementStock(variantId, quantity);
        
        if (!success) {
            throw new BadRequestException("Sản phẩm đã hết hàng (Redis Check).");
        }

        // 3. [ASYNC SYNC] Nếu Redis OK -> Đẩy việc trừ DB cho Worker
        // Main thread sẽ đi tiếp ngay lập tức -> Tạo đơn hàng rất nhanh
        inventorySyncWorker.syncDecreaseStock(variantId, quantity);
    }

    @Override
    public void restoreStock(Long variantId, Integer quantity) {
        log.info("Hoàn hàng cho Variant: {}", variantId);
        
        // 1. Hoàn kho Redis ngay lập tức để người khác mua được
        inventoryRedisService.incrementStock(variantId, quantity);
        
        // 2. Đồng bộ DB bất đồng bộ
        inventorySyncWorker.syncIncreaseStock(variantId, quantity);
    }
}