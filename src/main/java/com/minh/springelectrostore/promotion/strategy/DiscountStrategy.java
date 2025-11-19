package com.minh.springelectrostore.promotion.strategy;

import com.minh.springelectrostore.promotion.entity.Voucher;
import java.math.BigDecimal;

public interface DiscountStrategy {
    /**
     * Tính toán số tiền được giảm.
     * @param voucher Thông tin voucher
     * @param orderTotal Tổng tiền đơn hàng (trước giảm giá)
     * @return Số tiền được giảm (VND)
     */
    BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal);
}