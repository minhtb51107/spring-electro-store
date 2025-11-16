package com.minh.springelectrostore.user.service;

import com.minh.springelectrostore.user.dto.request.AddressRequest;
import com.minh.springelectrostore.user.dto.response.AddressResponse;
import java.util.List;

public interface AddressService {
    List<AddressResponse> getMyAddresses(String userEmail);
    AddressResponse createAddress(String userEmail, AddressRequest request);
    AddressResponse updateAddress(String userEmail, Long addressId, AddressRequest request);
    void deleteAddress(String userEmail, Long addressId);
    
    // Hàm helper để lấy địa chỉ mặc định khi checkout (nếu user không chọn)
    AddressResponse getDefaultAddress(String userEmail);
}