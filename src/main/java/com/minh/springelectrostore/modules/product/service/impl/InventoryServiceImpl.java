package com.minh.springelectrostore.modules.product.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.product.service.InventoryService;
import com.minh.springelectrostore.modules.product.worker.InventorySyncWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryRedisService inventoryRedisService;
    private final InventorySyncWorker inventorySyncWorker;

    @Override
    public void reserveStock(Long variantId, Integer quantity) {
        // 1. [Fail-safe] Nếu Redis chưa có key, load từ DB lên
        if (!inventoryRedisService.hasStockKey(variantId)) {
            log.info("Cache Miss: Nạp tồn kho từ DB lên Redis cho Variant {}", variantId);
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại"));
            // Sử dụng forceUpdate để đảm bảo đồng bộ mới nhất
            inventoryRedisService.forceUpdateStock(variantId, variant.getStockQuantity());
        }

        // 2. [REDIS GATEKEEPER] Trừ kho trên Redis
        boolean success = inventoryRedisService.decrementStock(variantId, quantity);
        
        if (!success) {
            // Ném lỗi cụ thể để Controller bắt được và báo "Hết hàng"
            throw new BadRequestException("Sản phẩm (ID: " + variantId + ") tạm thời hết hàng.");
        }

        // 3. [ASYNC SYNC] Đẩy việc trừ DB cho Worker chạy ngầm
        // Lưu ý: Nếu Transaction DB ở OrderService bị rollback sau bước này, 
        // ta phải gọi restoreStock thủ công ở OrderService.
        inventorySyncWorker.syncDecreaseStock(variantId, quantity);
    }

    @Override
    public void restoreStock(Long variantId, Integer quantity) {
        log.info("Hoàn hàng cho Variant: {}", variantId);
        
        // 1. Hoàn kho Redis ngay lập tức để người khác mua được
        inventoryRedisService.incrementStock(variantId, quantity);
        
        // 2. Đồng bộ DB bất đồng bộ (trả lại số lượng vào MySQL)
        inventorySyncWorker.syncIncreaseStock(variantId, quantity);
    }
}