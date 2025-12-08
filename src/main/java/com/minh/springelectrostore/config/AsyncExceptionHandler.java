package com.minh.springelectrostore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("CRITICAL ASYNC ERROR - Method: {} - Message: {}", method.getName(), ex.getMessage());
        for (Object param : params) {
            log.error("Parameter value: {}", param);
        }
        // Ở đây có thể tích hợp gửi cảnh báo về Telegram/Slack cho Admin
    }
}