package com.minh.springelectrostore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootApplication(exclude = { RedisRepositoriesAutoConfiguration.class })
// SỬA LẠI ĐƯỜNG DẪN CÁC GÓI (Thêm .modules vào giữa)
@EnableJpaRepositories(basePackages = {
    "com.minh.springelectrostore.modules.user.repository",
    "com.minh.springelectrostore.modules.auth.repository",
    "com.minh.springelectrostore.modules.product.repository",
    "com.minh.springelectrostore.modules.order.repository",
    "com.minh.springelectrostore.modules.promotion.repository",
    "com.minh.springelectrostore.modules.cart.repository", // Thêm nếu có
    "com.minh.springelectrostore.modules.habit.repository", // Thêm nếu có
    "com.minh.springelectrostore.modules.gamification.repository" // Thêm nếu có
})
public class SpringElectroStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringElectroStoreApplication.class, args);
    }
//    
//    @Bean
//    public CommandLineRunner flushRedisOnStartup(RedisConnectionFactory connectionFactory) {
//        return args -> {
//            try {
//                connectionFactory.getConnection().serverCommands().flushAll();
//                System.out.println("===========================================");
//                System.out.println(">>> ĐÃ XÓA SẠCH DỮ LIỆU REDIS (FLUSHALL) <<<");
//                System.out.println("===========================================");
//            } catch (Exception e) {
//                System.err.println(">>> KHÔNG THỂ XÓA REDIS: " + e.getMessage());
//            }
//        };
//    }
}