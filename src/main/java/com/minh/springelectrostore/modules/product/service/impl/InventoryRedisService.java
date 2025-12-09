package com.minh.springelectrostore.modules.product.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String STOCK_KEY_PREFIX = "product:stock:";

    /**
     * Khởi tạo tồn kho vào Redis (Warm-up Cache)
     */
    public void setStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        redisTemplate.opsForValue().set(key, quantity);
        // Không set TTL hoặc set TTL rất dài vì đây là dữ liệu quan trọng
    }

    /**
     * Trừ kho Atomic trên Redis (Gatekeeper)
     * @return true nếu trừ thành công, false nếu hết hàng
     */
    public boolean decrementStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        
        // 1. Decrement Atomic
        // Nếu key chưa tồn tại, Redis sẽ coi là 0 -> -quantity (vẫn hoạt động đúng logic check < 0)
        Long newStock = redisTemplate.opsForValue().decrement(key, quantity);
        
        // 2. Kiểm tra kết quả
        if (newStock != null && newStock >= 0) {
            log.info("Redis Stock OK: Variant {} còn {}", variantId, newStock);
            return true;
        } else {
            // 3. Compensation (Bù trừ): Nếu âm thì cộng lại để hoàn tác
            redisTemplate.opsForValue().increment(key, quantity);
            log.warn("Redis Stock FAIL: Variant {} đã hết hàng (Stock < 0)", variantId);
            return false;
        }
    }

    /**
     * Hoàn kho trên Redis (khi hủy đơn)
     */
    public void incrementStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        redisTemplate.opsForValue().increment(key, quantity);
        log.info("Đã hoàn kho Redis cho Variant {}: +{}", variantId, quantity);
    }
    
    /**
     * Kiểm tra xem Key đã tồn tại chưa (để Warm-up)
     */
    public boolean hasStockKey(Long variantId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(STOCK_KEY_PREFIX + variantId));
    }
}