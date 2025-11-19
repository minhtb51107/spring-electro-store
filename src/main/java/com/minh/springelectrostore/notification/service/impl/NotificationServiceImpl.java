package com.minh.springelectrostore.notification.service.impl;

import com.minh.springelectrostore.notification.dto.response.NotificationResponse;
import com.minh.springelectrostore.notification.service.NotificationService;
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

    @Override
    public void sendNotificationToUser(String email, String message) {
        NotificationResponse response = NotificationResponse.builder()
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();

        // Gửi đến: /user/{email}/queue/notifications
        // Frontend sẽ subscribe vào: /user/queue/notifications
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", response);
        
        log.info("Sent WebSocket notification to user: {}", email);
    }

    @Override
    public void sendGlobalNotification(String message) {
        NotificationResponse response = NotificationResponse.builder()
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();

        // Gửi đến: /topic/global-notifications
        messagingTemplate.convertAndSend("/topic/global-notifications", response);
    }
}