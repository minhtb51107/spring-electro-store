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
    
    // Set TTL 7 ngày để tránh rác Redis, nhưng đủ lâu để cache hit liên tục
    private static final long DEFAULT_TTL_DAYS = 7; 

    /**
     * Khởi tạo tồn kho vào Redis (Warm-up Cache)
     * Chỉ set nếu key chưa tồn tại để tránh ghi đè sai dữ liệu đang có
     */
    public void setStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        redisTemplate.opsForValue().setIfAbsent(key, quantity, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
    }
    
    /**
     * Cập nhật lại kho (dùng khi sync từ DB lên đè Redis)
     */
    public void forceUpdateStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        redisTemplate.opsForValue().set(key, quantity, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * Trừ kho Atomic trên Redis (Gatekeeper)
     * @return true nếu trừ thành công, false nếu hết hàng
     */
    public boolean decrementStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        
        // 1. Decrement Atomic: Redis thực hiện trừ và trả về giá trị mới ngay lập tức
        Long newStock = redisTemplate.opsForValue().decrement(key, quantity);
        
        // 2. Kiểm tra kết quả
        if (newStock != null && newStock >= 0) {
            // Gia hạn TTL mỗi khi có tương tác để key luôn "nóng"
            redisTemplate.expire(key, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
            log.info("Redis Stock OK: Variant {} còn {}", variantId, newStock);
            return true;
        } else {
            // 3. Compensation (Bù trừ): Nếu âm thì cộng lại ngay lập tức để hoàn tác
            redisTemplate.opsForValue().increment(key, quantity);
            log.warn("Redis Stock FAIL: Variant {} đã hết hàng (Stock < 0)", variantId);
            return false;
        }
    }

    /**
     * Hoàn kho trên Redis (khi hủy đơn hoặc rollback)
     */
    public void incrementStock(Long variantId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + variantId;
        redisTemplate.opsForValue().increment(key, quantity);
        redisTemplate.expire(key, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
        log.info("Đã hoàn kho Redis cho Variant {}: +{}", variantId, quantity);
    }
    
    public boolean hasStockKey(Long variantId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(STOCK_KEY_PREFIX + variantId));
    }
    
    public void deleteStockKey(Long variantId) {
        redisTemplate.delete(STOCK_KEY_PREFIX + variantId);
    }
}