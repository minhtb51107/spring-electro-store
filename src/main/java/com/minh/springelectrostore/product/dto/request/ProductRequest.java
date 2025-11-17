package com.minh.springelectrostore.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder; // <-- Import này
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // <-- THÊM DÒNG NÀY (Bắt buộc khi dùng @Builder.Default)
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;
    
    private String shortDescription;

    @NotNull(message = "Danh mục (category) không được để trống")
    private Long categoryId;

    @NotNull(message = "Thương hiệu (brand) không được để trống")
    private Long brandId;

    @Builder.Default // Giờ dòng này sẽ hoạt động đúng
    private boolean active = true;

    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 biến thể (variant)")
    @Valid
    private List<ProductVariantRequest> variants;
}