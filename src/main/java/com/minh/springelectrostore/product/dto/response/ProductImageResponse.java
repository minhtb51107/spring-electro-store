package com.minh.springelectrostore.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String imageUrl;
    private boolean thumbnail;
}