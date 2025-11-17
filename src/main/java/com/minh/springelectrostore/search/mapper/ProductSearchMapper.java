package com.minh.springelectrostore.search.mapper;

import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.search.document.ProductDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    /**
     * Chuyển đổi từ Document (Elasticsearch) sang DTO tóm tắt (API Response).
     * Đây là một mapping "dẹt" (flat) 1-1 đơn giản,
     * vì ProductDocument đã được phi chuẩn hóa (denormalized)
     * để chứa tất cả thông tin cần thiết (tên, giá, ảnh...).
     */
    @Mapping(source = "price", target = "price")
    @Mapping(source = "thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "categoryName", target = "categoryName")
    @Mapping(source = "brandName", target = "brandName")
    ProductSummaryResponse toSummaryResponse(ProductDocument document);
}