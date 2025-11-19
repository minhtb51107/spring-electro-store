package com.minh.springelectrostore.promotion.repository;

import com.minh.springelectrostore.promotion.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);

    /**
     * Tăng số lần sử dụng một cách an toàn (Atomic Update).
     * Chỉ tăng nếu chưa đạt giới hạn (usageLimit == 0 nghĩa là không giới hạn).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id AND (v.usageLimit = 0 OR v.usedCount < v.usageLimit)")
    int incrementUsage(@Param("id") Long id);

    /**
     * Hoàn lại lượt dùng (khi hủy đơn).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 " +
           "WHERE v.id = :id AND v.usedCount > 0")
    void decrementUsage(@Param("id") Long id);
}