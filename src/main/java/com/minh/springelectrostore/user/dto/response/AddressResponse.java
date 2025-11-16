package com.minh.springelectrostore.user.dto.response;

import lombok.Data;

@Data
public class AddressResponse {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String streetAddress;
    private String ward;
    private String district;
    private String province;
    private boolean isDefault;
    
    // Helper method để hiển thị full địa chỉ cho đẹp
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", streetAddress, ward, district, province);
    }
}