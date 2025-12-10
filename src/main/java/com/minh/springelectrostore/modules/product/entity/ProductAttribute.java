package com.minh.springelectrostore.modules.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ví dụ: "Màn hình", "Chipset", "RAM", "Pin"
    @Column(name = "attribute_name", nullable = false)
    private String name;

    // Ví dụ: "OLED 6.1 inch", "A15 Bionic", "8GB", "3000mAh"
    @Column(name = "attribute_value", nullable = false)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
}