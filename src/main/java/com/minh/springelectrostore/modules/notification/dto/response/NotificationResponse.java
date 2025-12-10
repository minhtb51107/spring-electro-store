package com.minh.springelectrostore.modules.notification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class NotificationResponse {
    private String message;
    private OffsetDateTime timestamp;
    
    private boolean isRead; 
    
    // Bạn có thể mở rộng thêm sau này nếu cần:
    // private String type; // ORDER, SYSTEM, PROMOTION...
    // private String link; // /orders/123
}