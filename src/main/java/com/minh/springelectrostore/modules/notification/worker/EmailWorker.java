package com.minh.springelectrostore.modules.notification.worker;

import com.minh.springelectrostore.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final EmailService emailService;

    /**
     * Hàm này sẽ chạy trên một luồng (Thread) riêng biệt.
     * Người dùng không phải chờ hàm này chạy xong.
     */
    @Async("taskExecutor") // Chỉ định rõ Executor đã cấu hình trong AsyncConfig
    public void sendOrderConfirmationEmail(Long orderId, String toEmail, String subject, String content) {
        log.info("[Async-Worker] Bắt đầu tác vụ gửi email cho Order #{}", orderId);
        long start = System.currentTimeMillis();

        try {
            // Giả lập độ trễ mạng nếu cần test
            // Thread.sleep(2000); 
            
            emailService.sendEmail(toEmail, subject, content);
            
            log.info("[Async-Worker] Gửi email thành công cho Order #{} (Mất {}ms)", 
                     orderId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[Async-Worker] LỖI gửi email cho Order #{}", orderId, e);
            // Có thể thêm logic Retry (thử lại) hoặc đẩy vào Queue lỗi tại đây
        }
    }
}