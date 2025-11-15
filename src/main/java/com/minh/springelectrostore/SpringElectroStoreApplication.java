package com.minh.springelectrostore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration; // 1. Import này
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 2. Thêm exclude = RedisRepositoriesAutoConfiguration.class
@SpringBootApplication(exclude = { RedisRepositoriesAutoConfiguration.class })
@EnableJpaRepositories(basePackages = {
    "com.minh.springelectrostore.user.repository",
    "com.minh.springelectrostore.auth.repository",
    "com.minh.springelectrostore.product.repository",
    "com.minh.springelectrostore.order.repository",
    "com.minh.springelectrostore.promotion.repository"
})
public class SpringElectroStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringElectroStoreApplication.class, args);
    }
}