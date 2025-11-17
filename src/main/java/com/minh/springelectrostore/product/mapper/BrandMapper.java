package com.minh.springelectrostore.product.mapper;

import com.minh.springelectrostore.product.dto.request.BrandRequest;
import com.minh.springelectrostore.product.dto.response.BrandResponse;
import com.minh.springelectrostore.product.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring") // Báo MapStruct tạo Spring Bean
public interface BrandMapper {

    /**
     * Chuyển từ DTO Request sang Entity.
     * Bỏ qua 'id' và 'slug' vì chúng sẽ được xử lý tự động trong Service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Brand toBrand(BrandRequest request);

    /**
     * Chuyển từ Entity sang DTO Response.
     */
    BrandResponse toBrandResponse(Brand brand);

    /**
     * Cập nhật Entity từ DTO Request (dùng cho hàm Update).
     * Bỏ qua 'id' và 'slug' để tránh ghi đè.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateBrandFromRequest(BrandRequest request, @MappingTarget Brand brand);
}