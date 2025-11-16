package com.minh.springelectrostore.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String receiverPhone;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    private String streetAddress;
    
    @NotBlank
    private String ward;
    @NotBlank
    private String district;
    @NotBlank
    private String province;
    
    private boolean isDefault;
    
    private Integer ghnDistrictId;
    private String ghnWardCode;
}