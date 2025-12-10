package com.minh.springelectrostore.modules.notification.listener;

import com.minh.springelectrostore.modules.notification.service.NotificationService;
import com.minh.springelectrostore.modules.notification.worker.EmailWorker;
import com.minh.springelectrostore.modules.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final EmailWorker emailWorker;
    private final NotificationService notificationService;

    /**
     * Lắng nghe sự kiện Order được tạo thành công
     * phase = AFTER_COMMIT: Chỉ chạy khi transaction DB đã commit xong
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Event received: Order #{} placed", event.getOrder().getId());

        String userEmail = event.getOrder().getCustomer().getUser().getEmail();
        String message = "Đơn hàng #" + event.getOrder().getId() + " đặt thành công! Chúng tôi đang xử lý.";

        // 1. [REAL-TIME] Gọi đúng hàm sendNotificationToUser như trong Interface hiện tại
        notificationService.sendNotificationToUser(userEmail, message);

        // 2. [BACKGROUND] Gửi Email (đã fix hàm sendEmail bên trong worker)
        emailWorker.sendOrderConfirmationEmail(userEmail, event.getOrder());
    }
}