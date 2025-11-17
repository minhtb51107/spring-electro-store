package com.minh.springelectrostore.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "slug", length = 100, nullable = false, unique = true)
    private String slug; // Dùng cho URL thân thiện (ví dụ: /brand/apple)

    @Size(max = 255)
    @Column(name = "logo_url", length = 255)
    private String logoUrl; // Đường dẫn tới ảnh logo

    // Trong tương lai, bạn có thể thêm:
    // @Column(columnDefinition = "TEXT")
    // private String description;
    
    // @OneToMany(mappedBy = "brand")
    // private List<Product> products;
}