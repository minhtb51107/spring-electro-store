package com.minh.springelectrostore.user.service.impl;

import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.dto.request.AddressRequest;
import com.minh.springelectrostore.user.dto.response.AddressResponse;
import com.minh.springelectrostore.user.entity.Address;
import com.minh.springelectrostore.user.entity.Customer;
import com.minh.springelectrostore.user.mapper.AddressMapper;
import com.minh.springelectrostore.user.repository.AddressRepository;
import com.minh.springelectrostore.user.repository.CustomerRepository;
import com.minh.springelectrostore.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(String userEmail) {
        Customer customer = getCustomerByEmail(userEmail);
        return addressRepository.findByCustomer_Id(customer.getId()).stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(String userEmail, AddressRequest request) {
        Customer customer = getCustomerByEmail(userEmail);
        
        // Nếu đây là địa chỉ đầu tiên, tự động set mặc định
        if (addressRepository.findByCustomer_Id(customer.getId()).isEmpty()) {
            request.setDefault(true);
        }

        // Nếu user muốn set địa chỉ này là mặc định -> Bỏ mặc định của cái cũ
        if (request.isDefault()) {
            unsetOldDefault(customer.getId());
        }

        Address address = addressMapper.toEntity(request);
        address.setCustomer(customer);
        
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userEmail, Long addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Bảo mật: Check quyền sở hữu
        if (!address.getCustomer().getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Bạn không có quyền sửa địa chỉ này");
        }

        // Logic đổi default: Nếu user muốn set thành default
        if (request.isDefault() && !address.isDefault()) {
            unsetOldDefault(address.getCustomer().getId());
        }
        // Logic đổi default: Nếu user muốn bỏ default -> Không cho phép bỏ trực tiếp
        // (Phải set cái khác làm default thì cái này mới mất) -> Giữ nguyên true
        else if (!request.isDefault() && address.isDefault()) {
            // Có thể throw exception hoặc lờ đi. Ở đây ta lờ đi và giữ nó là default.
            request.setDefault(true); 
        }

        addressMapper.updateEntity(request, address);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(String userEmail, Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow();
        
        if (!address.getCustomer().getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Không có quyền xóa");
        }
        
        // Không cho xóa địa chỉ mặc định (ép user phải chọn cái khác làm mặc định trước)
        if (address.isDefault()) {
            throw new IllegalStateException("Không thể xóa địa chỉ mặc định. Hãy đặt địa chỉ khác làm mặc định trước.");
        }

        addressRepository.delete(address);
    }
    
    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(String userEmail) {
        Customer customer = getCustomerByEmail(userEmail);
        return addressRepository.findByCustomer_IdAndIsDefaultTrue(customer.getId())
                .map(addressMapper::toResponse)
                .orElse(null);
    }

    // Helper
    private Customer getCustomerByEmail(String email) {
        return customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    // Helper: Bỏ tick default của địa chỉ cũ
    private void unsetOldDefault(Integer customerId) {
        addressRepository.findByCustomer_IdAndIsDefaultTrue(customerId)
                .ifPresent(addr -> {
                    addr.setDefault(false);
                    addressRepository.save(addr);
                });
    }
}