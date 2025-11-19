package com.minh.springelectrostore.promotion.entity;

import com.minh.springelectrostore.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "voucher_usages")
public class VoucherUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Liên kết với đơn hàng nào (để sau này hủy đơn thì biết đường hoàn lại)
    // (Tạm thời lưu orderId dạng Long để tránh vòng lặp dependency với module Order, 
    //  hoặc import Order entity nếu muốn chặt chẽ)
    private Long orderId; 

    @Column(name = "used_at")
    private OffsetDateTime usedAt;
}