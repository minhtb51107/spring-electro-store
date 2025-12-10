package com.minh.springelectrostore.modules.notification.service.impl;

import com.minh.springelectrostore.modules.notification.dto.response.NotificationResponse;
import com.minh.springelectrostore.modules.notification.service.NotificationService;
import com.minh.springelectrostore.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    // Inject thêm UserRepository để check user tồn tại (Optional, nhưng nên có để tránh gửi vào hư không)
    private final UserRepository userRepository;

    @Override
    public void sendNotificationToUser(String email, String message) {
        // 1. Validate: Kiểm tra xem user có tồn tại không trước khi bắn socket
        // Nếu hệ thống tải cao, có thể bỏ qua bước này để tối ưu tốc độ
        boolean userExists = userRepository.findByEmail(email).isPresent();
        if (!userExists) {
            log.warn("Không tìm thấy user [{}] để gửi thông báo.", email);
            return;
        }

        NotificationResponse response = NotificationResponse.builder()
                .message(message)
                .timestamp(OffsetDateTime.now())
                .isRead(false) // Thêm trường này vào DTO nếu chưa có (xem lưu ý bên dưới)
                .build();

        // 2. Gửi WebSocket: /user/{email}/queue/notifications
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", response);
        
        log.info("WS Notification sent to User [{}]: {}", email, message);
    }

    @Override
    public void sendGlobalNotification(String message) {
        NotificationResponse response = NotificationResponse.builder()
                .message(message)
                .timestamp(OffsetDateTime.now())
                .isRead(false)
                .build();

        // Gửi đến: /topic/global-notifications
        messagingTemplate.convertAndSend("/topic/global-notifications", response);
        log.info("WS Global Notification sent: {}", message);
    }
}