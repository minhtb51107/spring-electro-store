package com.minh.springelectrostore.product.mapper;

import com.minh.springelectrostore.product.dto.response.ReviewResponse;
import com.minh.springelectrostore.product.entity.ProductReview;
import com.minh.springelectrostore.product.entity.ReviewImage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "user.customer.fullname", target = "userName")
    // Xử lý ảnh đại diện: Nếu null thì trả về null (Frontend tự xử lý)
    @Mapping(source = "user.customer.photo", target = "userAvatar") 
    // Map danh sách ảnh từ Entity sang List<String>
    @Mapping(target = "images", expression = "java(mapImages(review))")
    ReviewResponse toResponse(ProductReview review);

    default List<String> mapImages(ProductReview review) {
        if (review.getImages() == null) return Collections.emptyList();
        return review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());
    }
}