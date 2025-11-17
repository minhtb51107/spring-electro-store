package com.minh.springelectrostore.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;

    private String color;
    private String storage;
    private String ram;

    private Set<ProductImageResponse> images;
}