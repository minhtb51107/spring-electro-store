package com.minh.springelectrostore.product.mapper;

import com.minh.springelectrostore.product.dto.request.CategoryRequest;
import com.minh.springelectrostore.product.dto.response.CategoryResponse;
import com.minh.springelectrostore.product.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Chuyển từ DTO Request sang Entity.
     * Bỏ qua tất cả các trường quan hệ (id, slug, parent, children).
     * Service sẽ xử lý các trường này thủ công.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toCategory(CategoryRequest request);

    /**
     * Chuyển từ Entity sang DTO Response.
     * Ánh xạ 'parent.id' (từ Entity Category) vào 'parentId' (của DTO Response).
     * Ánh xạ đệ quy cho 'children'.
     */
    @Mapping(source = "parent.id", target = "parentId")
    CategoryResponse toCategoryResponse(Category category);

    /**
     * Chuyển một Set<Entity> sang Set<DTO> (dùng cho 'children').
     */
    Set<CategoryResponse> toCategoryResponseSet(Set<Category> categories);

    /**
     * Cập nhật Entity từ DTO (dùng cho hàm Update).
     * Bỏ qua các trường quan hệ.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);
}