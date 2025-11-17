package com.minh.springelectrostore.product.mapper;

import com.minh.springelectrostore.product.dto.response.ProductDetailResponse;
import com.minh.springelectrostore.product.dto.response.ProductImageResponse;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.product.dto.response.ProductVariantResponse;
import com.minh.springelectrostore.product.entity.Product;
import com.minh.springelectrostore.product.entity.ProductImage;
import com.minh.springelectrostore.product.entity.ProductVariant;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

@Mapper(componentModel = "spring", 
        uses = {BrandMapper.class, CategoryMapper.class}) // <-- Sử dụng Mappers của Module 1
public interface ProductMapper {

    // --- MAPPING TỚI DETAIL RESPONSE (Đầy đủ) ---

    // Map Product -> ProductDetailResponse
    @Mapping(source = "category", target = "category") // Map lồng
    @Mapping(source = "brand", target = "brand")     // Map lồng
    ProductDetailResponse toDetailResponse(Product product);

    // Map ProductVariant -> ProductVariantResponse
    ProductVariantResponse toVariantResponse(ProductVariant variant);

    // Map ProductImage -> ProductImageResponse
    ProductImageResponse toImageResponse(ProductImage image);


    // --- MAPPING TỚI SUMMARY RESPONSE (Rút gọn) ---
    
    @Mappings({
        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "brand.name", target = "brandName"),
        @Mapping(target = "thumbnailUrl", ignore = true), // Sẽ xử lý thủ công ở dưới
        @Mapping(target = "price", ignore = true)        // Sẽ xử lý thủ công ở dưới
    })
    ProductSummaryResponse toSummaryResponse(Product product);

    /**
     * KỸ THUẬT NÂNG CAO: Tùy chỉnh logic mapping.
     * Sau khi MapStruct map các trường cơ bản (name, slug, categoryName...),
     * hàm này sẽ được gọi để xử lý logic phức tạp:
     * 1. Tìm giá thấp nhất trong các biến thể.
     * 2. Tìm ảnh thumbnail (hoặc ảnh đầu tiên) để hiển thị.
     */
    @AfterMapping
    default void afterMapToSummaryResponse(Product product, @MappingTarget ProductSummaryResponse.ProductSummaryResponseBuilder builder) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return; // Không có biến thể
        }

        // 1. Tìm giá thấp nhất
        Optional<BigDecimal> minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(Comparator.naturalOrder());
        
        minPrice.ifPresent(builder::price); // Set giá nếu tìm thấy

        // 2. Tìm ảnh thumbnail
        // Ưu tiên 1: Tìm ảnh có cờ is_thumbnail = true
        Optional<String> thumbnailUrl = product.getVariants().stream()
                .flatMap(variant -> variant.getImages().stream())
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst();

        // Ưu tiên 2: Nếu không có thumbnail, lấy ảnh bất kỳ đầu tiên
        if (thumbnailUrl.isEmpty()) {
            thumbnailUrl = product.getVariants().stream()
                    .flatMap(variant -> variant.getImages().stream())
                    .map(ProductImage::getImageUrl)
                    .findFirst();
        }
        
        thumbnailUrl.ifPresent(builder::thumbnailUrl); // Set ảnh nếu tìm thấy
    }
}