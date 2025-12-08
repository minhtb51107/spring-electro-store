package com.minh.springelectrostore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Thời gian chờ kết nối (3 giây)
        factory.setConnectTimeout(3000);
        
        // Thời gian chờ dữ liệu trả về (5 giây) - Tránh treo hệ thống nếu GHN xử lý lâu
        factory.setReadTimeout(5000);
        
        return new RestTemplate(factory);
    }
}