package com.minh.springelectrostore.modules.search.mapper;

import com.minh.springelectrostore.modules.product.entity.Product;
import com.minh.springelectrostore.modules.product.entity.ProductImage;
import com.minh.springelectrostore.modules.product.entity.ProductVariant;
import com.minh.springelectrostore.modules.search.document.ProductDocument;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.Named; // Import thêm nếu cần, ở đây dùng default method nên ko bắt buộc

import java.math.BigDecimal;
import java.time.Instant;        // [Import Mới]
import java.time.OffsetDateTime; // [Import Mới]
import java.util.Comparator;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductSyncMapper {

    @Mappings({
        @Mapping(source = "id", target = "id"),
        @Mapping(source = "name", target = "name"),
        @Mapping(source = "description", target = "description"),
        @Mapping(source = "slug", target = "slug"),
        @Mapping(source = "active", target = "active"),
        
        // MapStruct sẽ tự động tìm hàm map(OffsetDateTime) ở dưới để dùng cho dòng này
        @Mapping(source = "createdAt", target = "createdAt"),
        @Mapping(source = "updatedAt", target = "updatedAt"),

        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "brand.name", target = "brandName"),
        @Mapping(source = "category.slug", target = "categorySlug"),
        
        // Dòng này giờ sẽ hoạt động vì đã thêm brandSlug vào ProductDocument
        @Mapping(source = "brand.slug", target = "brandSlug"),
        
        @Mapping(source = "category.id", target = "categoryId"),
        @Mapping(source = "brand.id", target = "brandId"),

        @Mapping(target = "price", ignore = true),
        @Mapping(target = "salePrice", ignore = true),
        @Mapping(target = "thumbnail", ignore = true),     
        @Mapping(target = "stockQuantity", ignore = true)  
    })
    ProductDocument toDocument(Product product);

    // [FIX] Hàm chuyển đổi từ OffsetDateTime (JPA) sang Instant (Elasticsearch)
    default Instant map(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toInstant();
    }

    @AfterMapping
    default void afterMapToDocument(Product product, @MappingTarget ProductDocument.ProductDocumentBuilder builder) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            builder.price(BigDecimal.ZERO);
            builder.salePrice(BigDecimal.ZERO);
            builder.stockQuantity(0); 
            return; 
        }

        Optional<BigDecimal> minPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .min(Comparator.naturalOrder());
        
        BigDecimal price = minPrice.orElse(BigDecimal.ZERO);
        builder.price(price);
        builder.salePrice(price);

        int totalStock = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockQuantity)
                .sum();
        
        builder.stockQuantity(totalStock);

        Optional<String> thumbnail = product.getVariants().stream()
                .flatMap(variant -> variant.getImages().stream())
                .filter(ProductImage::isThumbnail)
                .map(ProductImage::getImageUrl)
                .findFirst();

        if (thumbnail.isEmpty()) {
            thumbnail = product.getVariants().stream()
                    .flatMap(variant -> variant.getImages().stream())
                    .map(ProductImage::getImageUrl)
                    .findFirst();
        }
        
        thumbnail.ifPresent(builder::thumbnail); 
    }
}