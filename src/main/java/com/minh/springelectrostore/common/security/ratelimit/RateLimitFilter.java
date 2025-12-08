package com.minh.springelectrostore.common.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisRateLimiter redisRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ipAddress = getClientIp(request);
        String uri = request.getRequestURI();

        // 1. Cấu hình Policy cho từng API quan trọng
        
        // Login & Register: Chống brute-force (cho phép 5 lần/phút)
        if (uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/register")) {
            if (!redisRateLimiter.isAllowed("auth", ipAddress, 5, Duration.ofMinutes(1))) {
                sendErrorResponse(response, "Bạn thao tác quá nhanh. Vui lòng thử lại sau 1 phút.");
                return;
            }
        }

        // Đặt hàng (Checkout): Chống spam đơn (cho phép 10 đơn/phút - hơi lỏng nhưng an toàn cho user thật)
        if (uri.startsWith("/api/v1/orders") && "POST".equalsIgnoreCase(request.getMethod())) {
            if (!redisRateLimiter.isAllowed("order", ipAddress, 10, Duration.ofMinutes(1))) {
                sendErrorResponse(response, "Hệ thống đang bận. Vui lòng đợi giây lát.");
                return;
            }
        }

        // Các API khác: Giới hạn chung (100 request/phút để chống DDoS nhẹ)
        if (!redisRateLimiter.isAllowed("global", ipAddress, 100, Duration.ofMinutes(1))) {
             sendErrorResponse(response, "Too many requests.");
             return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8"); // Đảm bảo tiếng Việt không lỗi font

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("message", message);
        
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || "".equals(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr;
    }
}