package com.minh.springelectrostore.modules.user.entity;

import com.minh.springelectrostore.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "wishlists", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"}) // Mỗi user chỉ thích 1 sản phẩm 1 lần
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // Eager để khi load wishlist thấy luôn sản phẩm
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "added_at")
    private Instant addedAt;

    @PrePersist
    public void prePersist() {
        this.addedAt = Instant.now();
    }
}