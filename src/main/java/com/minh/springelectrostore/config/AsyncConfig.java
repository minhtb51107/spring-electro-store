package com.minh.springelectrostore.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer; // Import này
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer { // Implement Interface này

    @Bean(name = "taskExecutor")
    @Override // Override method getAsyncExecutor
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20); // Tăng lên chút cho thoải mái
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ElectroWorker-");
        executor.initialize();
        return executor;
    }

    @Override // Override method xử lý lỗi
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }
}