package com.minh.springelectrostore.modules.user.mapper;

import org.springframework.stereotype.Component;

import com.minh.springelectrostore.modules.user.dto.response.UserActivityLogResponse;
import com.minh.springelectrostore.modules.user.entity.UserActivityLog;

@Component
public class UserActivityLogMapper {

    public UserActivityLogResponse toResponse(UserActivityLog log) {
        if (log == null) return null;
        return UserActivityLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .build();
    }
}