package com.minh.springelectrostore.shipping.service;

import java.math.BigDecimal;

public interface ShippingService {
    /**
     * Tính phí vận chuyển.
     * @param toDistrictId ID Quận/Huyện người nhận (theo chuẩn GHN)
     * @param toWardCode Mã Phường/Xã người nhận (theo chuẩn GHN)
     * @param weight Cân nặng gói hàng (gram)
     * @param orderValue Giá trị đơn hàng (để tính bảo hiểm)
     * @return Phí ship (VND)
     */
    BigDecimal calculateShippingFee(Integer toDistrictId, String toWardCode, Integer weight, Integer orderValue);
}