package com.minh.springelectrostore.modules.shipping.service.impl;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.modules.shipping.dto.request.GhnCalculateFeeRequest;
import com.minh.springelectrostore.modules.shipping.dto.response.GhnFeeResponse;
import com.minh.springelectrostore.modules.shipping.service.ShippingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker; // [Import Mới]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnShippingServiceImpl implements ShippingService {

    private final RestTemplate restTemplate;

    @Value("${ghn.api-url}")
    private String apiUrl;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Value("${ghn.sender-district-id}")
    private Integer senderDistrictId;

    @Override
    // [MỚI] Áp dụng Circuit Breaker với tên "ghnBackend" đã config ở properties
    // fallbackMethod: Tên hàm sẽ được gọi khi có lỗi hoặc Circuit đang mở
    @CircuitBreaker(name = "ghnBackend", fallbackMethod = "calculateShippingFeeFallback")
    public BigDecimal calculateShippingFee(Integer toDistrictId, String toWardCode, Integer weight, Integer orderValue) {
        // ... (Giữ nguyên toàn bộ logic cũ ở đây) ...
        String url = apiUrl + "/v2/shipping-order/fee";

        GhnCalculateFeeRequest request = GhnCalculateFeeRequest.builder()
                .serviceTypeId(2)
                .fromDistrictId(senderDistrictId)
                .toDistrictId(toDistrictId)
                .toWardCode(toWardCode)
                .height(10).length(10).width(10)
                .weight(weight)
                .insuranceValue(orderValue)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", token);
        headers.set("ShopId", shopId);

        HttpEntity<GhnCalculateFeeRequest> entity = new HttpEntity<>(request, headers);

        try {
            GhnFeeResponse response = restTemplate.postForObject(url, entity, GhnFeeResponse.class);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return BigDecimal.valueOf(response.getData().getTotal());
            } else {
                String msg = response != null ? response.getMessage() : "Lỗi không xác định từ GHN";
                log.error("GHN API trả lỗi logic: {}", msg);
                throw new BadRequestException("GHN Error: " + msg);
            }
        } catch (Exception e) {
            log.error("Lỗi kết nối GHN: {}", e.getMessage());
            throw e; // Ném lỗi ra để Circuit Breaker bắt được và kích hoạt Fallback
        }
    }

    // [MỚI] Hàm Fallback
    // Phải có cùng signature (tham số, kiểu trả về) với hàm chính + tham số Throwable
    public BigDecimal calculateShippingFeeFallback(Integer toDistrictId, String toWardCode, Integer weight, Integer orderValue, Throwable t) {
        log.warn("[Fallback] GHN đang gặp sự cố hoặc quá tải: {}. Sử dụng phí ship mặc định.", t.getMessage());
        
        // Return phí ship mặc định (ví dụ 30k) để User vẫn đặt hàng được
        return BigDecimal.valueOf(30000);
    }
}