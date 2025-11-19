package com.minh.springelectrostore.promotion.repository;

import com.minh.springelectrostore.promotion.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    
    // Kiểm tra xem user đã dùng voucher này chưa
    boolean existsByUserIdAndVoucherId(Integer userId, Long voucherId);

    // Tìm lịch sử dùng theo orderId để hoàn tác
    Optional<VoucherUsage> findByOrderId(Long orderId);
}