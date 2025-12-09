package com.minh.springelectrostore.modules.product.controller;

import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.modules.product.service.impl.InventoryRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryRedisService inventoryRedisService;

    @PostMapping("/warm-up")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin được gọi
    public ResponseEntity<String> warmUpCache() {
        List<ProductVariant> variants = productVariantRepository.findAll();
        int count = 0;
        
        for (ProductVariant variant : variants) {
            inventoryRedisService.setStock(variant.getId(), variant.getStockQuantity());
            count++;
        }
        
        return ResponseEntity.ok("Đã nạp tồn kho vào Redis cho " + count + " sản phẩm.");
    }
}