package com.minh.springelectrostore.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Mã giảm giá (ví dụ: TET2025)

    @Column(nullable = false)
    private String description; // Mô tả

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType; // PERCENTAGE, FIXED_AMOUNT

    @Column(nullable = false)
    private BigDecimal discountValue; // Giá trị (ví dụ: 10 (%), 50000 (VND))

    private BigDecimal maxDiscountAmount; // Giảm tối đa (chỉ dùng cho PERCENTAGE)
    
    private BigDecimal minOrderAmount; // Giá trị đơn hàng tối thiểu để áp dụng

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    @Builder.Default
    private Integer usageLimit = 0; // Tổng số lần có thể dùng

    @Builder.Default
    private Integer usedCount = 0; // Số lần đã dùng

    @Builder.Default
    private boolean active = true;
}