package com.minh.springelectrostore.user.controller;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minh.springelectrostore.user.dto.response.UserActivityLogResponse;
import com.minh.springelectrostore.user.service.UserActivityLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserActivityLogController {

    private final UserActivityLogService userActivityLogService;

    @GetMapping
    public ResponseEntity<Page<UserActivityLogResponse>> searchActivityLogs(
            // --- SỬA: THÊM name="..." CHO TẤT CẢ @RequestParam ---
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            // -----------------------------------------------------
            Pageable pageable) {

        Page<UserActivityLogResponse> logs = userActivityLogService.searchLogs(userId, action, startDate, endDate, pageable);
        return ResponseEntity.ok(logs);
    }
}