package com.minh.springelectrostore.modules.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
@Setting(settingPath = "/static/es-settings.json") 
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "vn_unaccent_analyzer")
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal salePrice;

    @Field(type = FieldType.Keyword)
    private String thumbnail;

    @Field(type = FieldType.Text, analyzer = "vn_unaccent_analyzer")
    private String description;

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Long)
    private Long soldQuantity;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    // --- Category Fields ---
    @Field(type = FieldType.Long)
    private Long categoryId;
    
    @Field(type = FieldType.Keyword)
    private String categoryName; 
    
    @Field(type = FieldType.Keyword)
    private String categorySlug;
    
    // --- Brand Fields ---
    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    // [FIX] Thêm trường này để sửa lỗi "Unknown property brandSlug"
    @Field(type = FieldType.Keyword)
    private String brandSlug;
}