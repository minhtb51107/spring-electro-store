package com.minh.springelectrostore.order.entity;

import com.minh.springelectrostore.user.entity.Customer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank
    @Column(nullable = false)
    private String shippingAddress;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String shippingPhone;

    @NotBlank
    @Column(nullable = false)
    private String customerName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPrice; // Tổng tiền hàng (trước khi giảm)
    
    @Column(name = "shipping_fee", precision = 19, scale = 4)
    private BigDecimal shippingFee;

    // --- CÁC TRƯỜNG BỔ SUNG CHO MODULE VOUCHER ---
    
    @Column(name = "voucher_code")
    private String voucherCode; // Mã voucher đã dùng

    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount; // Số tiền được giảm

    @Column(name = "final_price", precision = 19, scale = 4)
    private BigDecimal finalPrice; // Số tiền khách phải trả (total - discount)

    // ---------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}