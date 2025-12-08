package com.minh.springelectrostore.modules.notification.worker;

import com.minh.springelectrostore.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {

    private final EmailService emailService;

    @Async("taskExecutor")
    // [UPDATE] Thêm Annotation Retryable
    // Nếu gặp Exception bất kỳ, thử lại tối đa 3 lần, delay 2000ms (2 giây)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendOrderConfirmationEmail(Long orderId, String toEmail, String subject, String content) {
        log.info("[Async-Worker] Bắt đầu tác vụ gửi email cho Order #{}", orderId);
        
        // Nếu dòng này quăng lỗi, Spring sẽ tự động catch và chạy lại hàm này sau 2s
        emailService.sendEmail(toEmail, subject, content);
        
        log.info("[Async-Worker] Gửi email thành công cho Order #{}", orderId);
    }

    // [UPDATE] Hàm Recover: Chạy khi đã thử hết 3 lần mà vẫn lỗi
    @Recover
    public void recoverEmailFailure(Exception e, Long orderId, String toEmail, String subject, String content) {
        log.error("[Async-Worker] Đã thử 3 lần nhưng vẫn thất bại gửi email Order #{}. Lỗi: {}", orderId, e.getMessage());
        // TODO: Lưu vào bảng `notification_failed` trong DB để Admin xử lý thủ công sau này
    }
}