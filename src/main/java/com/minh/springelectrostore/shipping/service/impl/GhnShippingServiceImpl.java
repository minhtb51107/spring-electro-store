package com.minh.springelectrostore.shipping.service.impl;

import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shipping.dto.GhnCalculateFeeRequest;
import com.minh.springelectrostore.shipping.dto.GhnFeeResponse;
import com.minh.springelectrostore.shipping.service.ShippingService;
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

    private final RestTemplate restTemplate; // Cần bean RestTemplate trong Config

    @Value("${ghn.api-url}")
    private String apiUrl;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Value("${ghn.sender-district-id}")
    private Integer senderDistrictId;

    @Override
    public BigDecimal calculateShippingFee(Integer toDistrictId, String toWardCode, Integer weight, Integer orderValue) {
        String url = apiUrl + "/v2/shipping-order/fee";

        // 1. Build Request Body
        GhnCalculateFeeRequest request = GhnCalculateFeeRequest.builder()
                .serviceTypeId(2) // 2 = E-commerce Delivery (Chuẩn)
                .fromDistrictId(senderDistrictId)
                .toDistrictId(toDistrictId)
                .toWardCode(toWardCode)
                .height(10) // Giả định kích thước (cm)
                .length(10)
                .width(10)
                .weight(weight)
                .insuranceValue(orderValue)
                .build();

        // 2. Build Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", token);
        headers.set("ShopId", shopId);

        HttpEntity<GhnCalculateFeeRequest> entity = new HttpEntity<>(request, headers);

        // 3. Call API
        try {
            GhnFeeResponse response = restTemplate.postForObject(url, entity, GhnFeeResponse.class);

            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return BigDecimal.valueOf(response.getData().getTotal());
            } else {
                log.error("GHN Error: {}", response != null ? response.getMessage() : "Unknown");
                throw new BadRequestException("Không thể tính phí vận chuyển lúc này.");
            }
        } catch (Exception e) {
            log.error("Error calling GHN API", e);
            // Fallback: Trả về phí cố định nếu lỗi (ví dụ 30k) để không chặn user mua hàng
            return BigDecimal.valueOf(30000); 
        }
    }
}