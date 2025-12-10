package com.minh.springelectrostore.modules.product.controller;

import com.minh.springelectrostore.modules.product.dto.response.ProductComparisonResponse;
import com.minh.springelectrostore.modules.product.service.impl.ProductComparisonServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/compare")
@RequiredArgsConstructor
public class ProductComparisonController {

    private final ProductComparisonServiceImpl comparisonService;

    // GET /api/v1/products/compare?ids=1,2,3
    @GetMapping
    public ResponseEntity<ProductComparisonResponse> compare(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(comparisonService.compareProducts(ids));
    }
}