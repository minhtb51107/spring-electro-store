package com.minh.springelectrostore.notification.service;

import com.minh.springelectrostore.notification.dto.response.NotificationResponse;

public interface NotificationService {
    
    /**
     * Gửi thông báo đến một user cụ thể qua WebSocket.
     * @param email Email của user nhận (username).
     * @param message Nội dung thông báo.
     */
    void sendNotificationToUser(String email, String message);

    /**
     * Gửi thông báo chung cho tất cả user (ví dụ: Khuyến mãi mới).
     */
    void sendGlobalNotification(String message);
}