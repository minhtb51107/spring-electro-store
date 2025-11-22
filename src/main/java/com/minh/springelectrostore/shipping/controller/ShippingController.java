package com.minh.springelectrostore.shipping.controller;

import com.minh.springelectrostore.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
}