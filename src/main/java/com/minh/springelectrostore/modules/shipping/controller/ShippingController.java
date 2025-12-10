package com.minh.springelectrostore.modules.shipping.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.minh.springelectrostore.modules.shipping.service.ShippingService;

import java.math.BigDecimal;

import com.minh.springelectrostore.modules.shipping.dto.request.GhnCalculateFeeRequest;
import com.minh.springelectrostore.modules.shipping.dto.response.GhnFeeResponse;

import jakarta.validation.Valid; // Nhớ import validation

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/calculate")
    public ResponseEntity<BigDecimal> calculateFee(
            @RequestParam Integer districtId,
            @RequestParam String wardCode,
            @RequestParam(defaultValue = "1000") Integer weight, // Mặc định 1kg
            @RequestParam(defaultValue = "0") Integer insuranceValue
    ) {
        BigDecimal fee = shippingService.calculateShippingFee(districtId, wardCode, weight, insuranceValue);
        return ResponseEntity.ok(fee);
    }
    
    @PostMapping("/calculate-public")
    public ResponseEntity<GhnFeeResponse> calculateFeePublic(@RequestBody @Valid GhnCalculateFeeRequest request) {
        // [FIX 1] Sử dụng getter chuẩn CamelCase của Lombok
        int weight = request.getWeight() != null ? request.getWeight() : 500;
        int insuranceValue = request.getInsuranceValue() != null ? request.getInsuranceValue() : 0;

        // Gọi service GHN
        BigDecimal fee = shippingService.calculateShippingFee(
            request.getToDistrictId(), // [FIX 1] getToDistrictId()
            request.getToWardCode(),   // [FIX 1] getToWardCode()
            weight,
            insuranceValue
        );

        // [FIX 2] Cấu trúc lại Response đúng theo nested class GhnFeeData
        GhnFeeResponse response = new GhnFeeResponse();
        response.setCode(200);
        response.setMessage("Success");
        
        GhnFeeResponse.GhnFeeData data = new GhnFeeResponse.GhnFeeData();
        data.setTotal(fee.intValue()); // Convert BigDecimal sang Integer
        
        response.setData(data);
        
        return ResponseEntity.ok(response);
    }
}