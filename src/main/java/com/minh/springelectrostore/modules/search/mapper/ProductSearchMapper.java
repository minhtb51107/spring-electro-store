package com.minh.springelectrostore.modules.search.mapper;

import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.modules.search.document.ProductDocument;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    /**
     * Chuyển đổi từ Document (Elasticsearch) sang DTO tóm tắt.
     */
    @Mapping(source = "price", target = "price")
    // [FIX] Đổi source từ 'thumbnailUrl' thành 'thumbnail' cho khớp với ProductDocument
    @Mapping(source = "thumbnail", target = "thumbnailUrl") 
    @Mapping(source = "categoryName", target = "categoryName")
    @Mapping(source = "brandName", target = "brandName")
    ProductSummaryResponse toSummaryResponse(ProductDocument document);
}