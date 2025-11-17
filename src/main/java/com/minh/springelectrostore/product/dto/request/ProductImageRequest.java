package com.minh.springelectrostore.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {

    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imageUrl; // URL này sẽ lấy từ Cloudinary/S3 (Module 9)

    private boolean thumbnail = false; // Mặc định không phải là ảnh đại diện
}