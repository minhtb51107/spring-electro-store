package com.minh.springelectrostore.user.controller;

import java.util.List; // *** THÊM IMPORT ***

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minh.springelectrostore.user.dto.request.UpdateProfileRequest;
import com.minh.springelectrostore.user.dto.response.ProfileResponse;
import com.minh.springelectrostore.user.dto.response.UserStatsResponse;
import com.minh.springelectrostore.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Yêu cầu đăng nhập
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        String userEmail = authentication.getName();
        ProfileResponse profile = userService.getUserProfile(userEmail);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Yêu cầu đăng nhập
    public ResponseEntity<ProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userEmail = authentication.getName();
        ProfileResponse updatedProfile = userService.updateUserProfile(userEmail, request);
        return ResponseEntity.ok(updatedProfile);
    }
}