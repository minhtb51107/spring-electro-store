package com.minh.springelectrostore.product.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    private String comment;
    
    private List<String> imageUrls;
}