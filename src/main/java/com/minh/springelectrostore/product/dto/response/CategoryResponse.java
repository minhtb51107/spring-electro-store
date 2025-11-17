package com.minh.springelectrostore.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable; // Import
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse implements Serializable { // Thêm implements
    private static final long serialVersionUID = 1L; // Thêm ID

    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Set<CategoryResponse> children;
}