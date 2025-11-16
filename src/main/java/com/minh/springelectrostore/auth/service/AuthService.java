package com.minh.springelectrostore.auth.service;


import com.minh.springelectrostore.auth.dto.request.ChangePasswordRequest;
import com.minh.springelectrostore.auth.dto.request.ForgotPasswordRequest;
import com.minh.springelectrostore.auth.dto.request.LoginRequest;
import com.minh.springelectrostore.auth.dto.request.RegisterRequest;
import com.minh.springelectrostore.auth.dto.request.ResetPasswordRequest;
import com.minh.springelectrostore.auth.dto.response.JwtResponse;
import com.minh.springelectrostore.user.dto.response.UserDetailsResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    void registerCustomer(RegisterRequest request);

    void activateUserAccount(String token);

    JwtResponse login(LoginRequest request, HttpServletRequest servletRequest);

    JwtResponse refreshToken(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String userEmail);

    UserDetailsResponse getCurrentUserDetails(String userEmail);

    void logout(String refreshToken);

    JwtResponse loginWithGoogle(String idTokenString, HttpServletRequest servletRequest);
}
