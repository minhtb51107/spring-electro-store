package com.minh.springelectrostore.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal; // Dùng BigDecimal cho tiền tệ
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết ngược lại với Product (cha)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // SKU (Stock Keeping Unit) - Mã định danh sản phẩm duy nhất
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotNull
    @Min(0)
    @Column(nullable = false, precision = 19, scale = 4) // Dùng cho tiền tệ
    private BigDecimal price;

    @Builder.Default
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    // Các thuộc tính của biến thể
    // Ví dụ: "Titan Xanh", "16GB-RAM", "1TB-SSD"
    @Size(max = 100)
    private String color;
    
    @Size(max = 50)
    private String storage;
    
    @Size(max = 50)
    private String ram;
    // (Bạn có thể mở rộng bằng cách tạo bảng ProductAttribute nếu muốn)

    // Một biến thể có nhiều hình ảnh
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProductImage> images = new HashSet<>();

    // Helper methods
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProductVariant(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProductVariant(null);
    }
}