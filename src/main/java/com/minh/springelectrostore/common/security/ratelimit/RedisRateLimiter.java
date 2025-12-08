package com.minh.springelectrostore.common.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Kiểm tra xem request có được phép đi tiếp không.
     * @param keyPrefix Tiền tố key (ví dụ: "login", "order")
     * @param identifier Định danh người dùng (IP hoặc UserID)
     * @param maxRequests Số request tối đa cho phép
     * @param duration Thời gian cửa sổ (ví dụ: trong 1 phút)
     * @return true nếu được phép, false nếu bị chặn
     */
    public boolean isAllowed(String keyPrefix, String identifier, int maxRequests, Duration duration) {
        String key = "rate_limit:" + keyPrefix + ":" + identifier;
        
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount != null && currentCount == 1) {
                // Nếu là lần đầu tiên, set thời gian hết hạn cho key
                redisTemplate.expire(key, duration.getSeconds(), TimeUnit.SECONDS);
            }
            
            // Nếu số lượng request vượt quá giới hạn
            if (currentCount != null && currentCount > maxRequests) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Lỗi Redis Rate Limiter: {}", e.getMessage());
            // Fail-open: Nếu Redis chết, vẫn cho user đi tiếp để không chặn nhầm khách thật
            return true;
        }
    }
}