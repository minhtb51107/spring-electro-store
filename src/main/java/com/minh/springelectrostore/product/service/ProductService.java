package com.minh.springelectrostore.product.service;

import com.minh.springelectrostore.product.dto.request.ProductRequest;
import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.dto.response.ProductDetailResponse;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /**
     * API Lọc/Tìm kiếm động cho khách hàng.
     * Sử dụng JPA Specifications.
     * @param criteria DTO chứa các tiêu chí lọc (category, brand, price...)
     * @param pageable Thông tin phân trang (page, size, sort)
     * @return Một trang (Page) chứa các sản phẩm tóm tắt (Summary).
     */
    Page<ProductSummaryResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    /**
     * Lấy chi tiết một sản phẩm (dùng cho trang chi tiết sản phẩm).
     * @param slug Slug của sản phẩm (vi-du: "laptop-dell-xps-15")
     * @return DTO Chi tiết sản phẩm (Full data).
     */
    ProductDetailResponse getProductBySlug(String slug);

    /**
     * Lấy chi tiết một sản phẩm bằng ID (dùng cho Admin).
     * @param id ID của sản phẩm
     * @return DTO Chi tiết sản phẩm (Full data).
     */
    ProductDetailResponse getProductById(Long id);

    /**
     * Tạo một sản phẩm mới (bao gồm các biến thể và hình ảnh).
     * Dành cho Admin.
     * @param request DTO lồng nhau chứa toàn bộ thông tin sản phẩm mới.
     * @return DTO Chi tiết của sản phẩm vừa tạo.
     */
    ProductDetailResponse createProduct(ProductRequest request);

    /**
     * Cập nhật một sản phẩm.
     * Dành cho Admin.
     * @param id ID của sản phẩm cần cập nhật.
     * @param request DTO lồng nhau chứa thông tin cập nhật.
     * @return DTO Chi tiết của sản phẩm sau khi cập nhật.
     */
    ProductDetailResponse updateProduct(Long id, ProductRequest request);

    /**
     * Xóa mềm (hoặc ẩn) một sản phẩm.
     * Dành cho Admin.
     * @param id ID của sản phẩm cần xóa/ẩn.
     */
    void deleteProduct(Long id);
}