package com.minh.springelectrostore.order.entity;

import com.minh.springelectrostore.product.entity.ProductVariant; // <-- Dùng từ Module 2
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết ngược lại với Order (cha)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Liên kết với sản phẩm đã mua
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private int quantity;

    // --- KỸ THUẬT "SNAPSHOT" GIÁ ---
    @NotNull
    @Column(name = "price_at_purchase", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceAtPurchase;
    // --------------------------------
}