package com.minh.springelectrostore.modules.cart.job;

import com.minh.springelectrostore.modules.cart.dto.response.CartResponse;
import com.minh.springelectrostore.modules.notification.worker.EmailWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartJob {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailWorker emailWorker;

    private static final String ACTIVE_CARTS_KEY = "cart:active_users";
    private static final String CART_PREFIX = "cart:";
    
    // Cấu hình thời gian:
    // - Nhắc sau: 30 phút (kể từ lần cuối update giỏ)
    // - Hết hạn nhắc: 24 giờ (quá 1 ngày thì thôi không nhắc nữa cho đỡ phiền)
    private static final long REMIND_AFTER_MINUTES = 30;
    private static final long EXPIRE_AFTER_HOURS = 24;

    // Chạy mỗi 15 phút một lần (bạn có thể chỉnh cron tùy ý)
    @Scheduled(cron = "0 0/15 * * * ?")
    public void scanAbandonedCarts() {
        log.info("JOB: Bắt đầu quét giỏ hàng bị bỏ quên...");

        // 1. Lấy danh sách tất cả user đang có giỏ hàng (từ Set)
        Set<Object> users = redisTemplate.opsForSet().members(ACTIVE_CARTS_KEY);
        if (users == null || users.isEmpty()) {
            log.info("JOB: Không có user nào đang active cart.");
            return;
        }

        int count = 0;
        for (Object userEmailObj : users) {
            String email = (String) userEmailObj;
            String cartKey = CART_PREFIX + email;
            
            try {
                // Lấy giỏ hàng chi tiết
                CartResponse cart = (CartResponse) redisTemplate.opsForValue().get(cartKey);
                
                // Nếu giỏ hàng không tồn tại hoặc rỗng -> Xóa khỏi danh sách active để lần sau không quét nữa
                if (cart == null || cart.getItems().isEmpty()) {
                    redisTemplate.opsForSet().remove(ACTIVE_CARTS_KEY, email);
                    continue;
                }

                // 2. Kiểm tra điều kiện gửi mail
                if (shouldSendReminder(cart)) {
                    // 3. Gửi email (Async)
                    emailWorker.sendAbandonedCartEmail(email, cart);
                    
                    // 4. Cập nhật trạng thái đã gửi
                    cart.setReminderSent(true);
                    
                    // Lưu lại vào Redis (giữ nguyên TTL cũ khoảng 7 ngày)
                    redisTemplate.opsForValue().set(cartKey, cart, 7, TimeUnit.DAYS);
                    
                    log.info("JOB: Đã kích hoạt gửi mail nhắc nhở cho: {}", email);
                    count++;
                }
                
            } catch (Exception e) {
                log.error("JOB: Lỗi xử lý user {}", email, e);
            }
        }
        log.info("JOB: Hoàn tất quét. Đã gửi {} email nhắc nhở.", count);
    }

    private boolean shouldSendReminder(CartResponse cart) {
        // Nếu đã gửi rồi thì không gửi lại
        if (cart.isReminderSent()) {
            return false;
        }

        Instant lastUpdate = cart.getLastUpdatedAt();
        if (lastUpdate == null) return false;

        Instant now = Instant.now();
        long minutesDiff = Duration.between(lastUpdate, now).toMinutes();
        long hoursDiff = Duration.between(lastUpdate, now).toHours();

        // Logic: Chỉ gửi nếu giỏ hàng "ngủ" > 30p VÀ < 24h
        return minutesDiff >= REMIND_AFTER_MINUTES && hoursDiff < EXPIRE_AFTER_HOURS;
    }
}