package com.minh.springelectrostore.notification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class NotificationResponse {
    private String message;
    private OffsetDateTime timestamp;
    // Có thể thêm: type (ORDER_UPDATE, PROMOTION...), link (để click vào)
}