package com.minh.springelectrostore.user.mapper;

import org.springframework.stereotype.Component;

import com.minh.springelectrostore.user.dto.response.UserActivityLogResponse;
import com.minh.springelectrostore.user.entity.UserActivityLog;

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