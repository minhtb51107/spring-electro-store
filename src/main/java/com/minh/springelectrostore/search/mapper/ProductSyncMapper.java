package com.minh.springelectrostore.search.mapper;

import com.minh.springelectrostore.product.entity.Product;
import com.minh.springelectrostore.product.entity.ProductImage;
import com.minh.springelectrostore.product.entity.ProductVariant;
import com.minh.springelectrostore.search.document.ProductDocument;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

/**
 * Mapper "Pro" này dùng để biến đổi Product (JPA, chuẩn hóa) 
 * thành ProductDocument (Elasticsearch, phi chuẩn hóa)
 * để tối ưu cho tìm kiếm.
 */
@Mapper(componentModel = "spring")
public interface ProductSyncMapper {

	@Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "name", target = "name"),
        @Mapping(source = "description", target = "description"),
        @Mapping(source = "slug", target = "slug"),
        @Mapping(source = "active", target = "active"),
        
        // --- THÊM MAPPING CHO CREATED_AT ---
        @Mapping(source = "createdAt", target = "createdAt"),
        // -----------------------------------

        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "brand.name", target = "brandName"),
        @Mapping(source = "category.slug", target = "categorySlug"),
        @Mapping(source = "brand.slug", target = "brandSlug"),

        @Mapping(target = "price", ignore = true),
        @Mapping(target = "thumbnailUrl", ignore = true),
        @Mapping(target = "totalStock", ignore = true)
    })
    ProductDocument toDocument(Product product);

    /**
     * Hàm này được gọi sau khi các mapping cơ bản ở trên hoàn tất.
     * Nó thực hiện logic tính toán/tổng hợp phức tạp.
     */
    @AfterMapping
    default void afterMapToDocument(Product product, @MappingTarget ProductDocument.ProductDocumentBuilder builder) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            builder.price(BigDecimal.ZERO);
            builder.totalStock(0);
            return; 
        }

        // --- 1. Tính toán giá (lấy giá rẻ nhất) ---
        Optional<BigDecimal> minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(Comparator.naturalOrder());
        
        builder.price(minPrice.orElse(BigDecimal.ZERO));

        // --- 2. Tính toán tổng tồn kho ---
        int totalStock = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockQuantity)
                .sum();
        
        builder.totalStock(totalStock);

        // --- 3. Tìm ảnh Thumbnail (logic giống ProductMapper) ---
        Optional<String> thumbnailUrl = product.getVariants().stream()
                .flatMap(variant -> variant.getImages().stream())
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst();

        if (thumbnailUrl.isEmpty()) {
            thumbnailUrl = product.getVariants().stream()
                    .flatMap(variant -> variant.getImages().stream())
                    .map(ProductImage::getImageUrl)
                    .findFirst();
        }
        
        thumbnailUrl.ifPresent(builder::thumbnailUrl); // Set ảnh nếu tìm thấy
    }
}