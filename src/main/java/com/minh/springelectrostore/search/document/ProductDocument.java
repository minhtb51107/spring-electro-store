package com.minh.springelectrostore.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime; // <--- THÊM IMPORT

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products_index") 
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Keyword)
    private String categoryName;
    
    @Field(type = FieldType.Keyword)
    private String categorySlug; 

    @Field(type = FieldType.Keyword)
    private String brandSlug;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer)
    private Integer totalStock;

    @Field(type = FieldType.Boolean)
    private boolean active;

    // --- THÊM MỚI: Trường này bắt buộc để Sort ---
    @Field(type = FieldType.Date)
    private OffsetDateTime createdAt; 
}