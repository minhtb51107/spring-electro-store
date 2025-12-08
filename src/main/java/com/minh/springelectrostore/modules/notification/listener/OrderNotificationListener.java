package com.minh.springelectrostore.modules.notification.listener;

import com.minh.springelectrostore.modules.notification.worker.EmailWorker;
import com.minh.springelectrostore.modules.order.entity.Order;
import com.minh.springelectrostore.modules.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final EmailWorker emailWorker; // Inject Worker thay vì Service

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        Order order = event.getOrder();
        log.info("Listener nhận sự kiện Order #{} -> Chuyển giao cho EmailWorker", order.getId());

        // 1. Chuẩn bị dữ liệu
        String userEmail;
        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            userEmail = order.getCustomer().getUser().getEmail();
        } else {
            return;
        }

        String emailBody = String.format(
            "<h1>Đơn hàng #%d thành công</h1><p>Tổng tiền: %,.0f VND</p>",
            order.getId(), order.getFinalPrice()
        );

        // 2. Giao việc cho Worker (Hàm này trả về ngay lập tức, Worker chạy ngầm)
        emailWorker.sendOrderConfirmationEmail(
            order.getId(),
            userEmail, 
            "Xác nhận đơn hàng #" + order.getId(), 
            emailBody
        );
    }
}