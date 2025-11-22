package com.minh.springelectrostore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean; // <--- Thêm import
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate; // <--- Thêm import
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    // --- THÊM ĐOẠN NÀY ĐỂ TẠO BEAN RESTTEMPLATE ---
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}