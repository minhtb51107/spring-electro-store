package com.minh.springelectrostore.product.controller;

import com.minh.springelectrostore.product.dto.request.ProductRequest;
import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.dto.response.ProductDetailResponse;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.product.entity.Product; // Import mới
import com.minh.springelectrostore.product.repository.ProductRepository; // Import mới
import com.minh.springelectrostore.product.service.ProductService;
import com.minh.springelectrostore.search.service.ProductSearchService;
import com.minh.springelectrostore.search.service.ProductSyncService; // Import mới
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List; // Import mới

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;
    
    // --- THÊM 2 DEPENDENCY NÀY ---
    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService; 

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSummaryResponse>> searchProducts(
            @RequestParam(name = "q", required = false) String q,
            @ModelAttribute ProductSearchCriteria criteria,
            @PageableDefault(size = 12) Pageable pageable) {
        
        Page<ProductSummaryResponse> productPage = productSearchService.searchProducts(q, criteria, pageable);
        return ResponseEntity.ok(productPage);
    }

    // ... (Các API getById, create, update, delete giữ nguyên) ...
    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getProductBySlug(@PathVariable("slug") String slug) {
        ProductDetailResponse productDetail = productService.getProductBySlug(slug);
        return ResponseEntity.ok(productDetail);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductDetailResponse newProduct = productService.createProduct(request);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductDetailResponse updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDetailResponse> getProductByIdForAdmin(@PathVariable("id") Long id) {
        ProductDetailResponse productDetail = productService.getProductById(id);
        return ResponseEntity.ok(productDetail);
    }

    // --- THÊM API ĐỒNG BỘ DỮ LIỆU ---
    @PostMapping("/sync-all")
    public ResponseEntity<String> syncAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        int count = 0;
        for (Product p : allProducts) {
             // Gọi hàm indexProduct để đẩy data từ Postgres sang Elasticsearch
             productSyncService.indexProduct(p.getId());
             count++;
        }
        return ResponseEntity.ok("Đã đồng bộ thành công " + count + " sản phẩm sang Elasticsearch.");
    }
}