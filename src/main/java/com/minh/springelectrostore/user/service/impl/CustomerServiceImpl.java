package com.minh.springelectrostore.user.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.dto.request.UpdateCustomerRequest;
import com.minh.springelectrostore.user.dto.response.CustomerResponse;
import com.minh.springelectrostore.user.entity.Customer;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.entity.UserStatus;
import com.minh.springelectrostore.user.mapper.CustomerMapper;
import com.minh.springelectrostore.user.repository.CustomerRepository;
import com.minh.springelectrostore.user.repository.UserRepository;
import com.minh.springelectrostore.user.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Hầu hết các method là đọc, nên set readOnly=true
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository; // <-- THÊM DEPENDENCY NÀY

    @Override
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        
        return customerMapper.toCustomerResponse(customer);
    }

    @Override
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return customerPage.map(customerMapper::toCustomerResponse);
    }
    
    @Override
    public CustomerResponse updateCustomerInfo(Integer id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        // Kiểm tra logic nghiệp vụ (ví dụ: SĐT không được trùng)
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(customer.getPhoneNumber())) {
            if(customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BadRequestException("Số điện thoại đã tồn tại.");
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getFullname() != null) {
            customer.setFullname(request.getFullname());
        }
        if (request.getPhoto() != null) {
            customer.setPhoto(request.getPhoto());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Override
    @Transactional // Đảm bảo đây là một transaction ghi
    public void deleteCustomer(Integer id) {
        // 1. Tìm Customer
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        // 2. Lấy User liên quan và khóa tài khoản
        User user = customer.getUser();
        if (user != null) {
            user.setStatus(UserStatus.SUSPENDED); // Đổi trạng thái thành bị khóa
            userRepository.save(user);
        }

        // 3. Xóa Customer (nhưng không xóa User)
        // Hành động này an toàn vì chúng ta đã gỡ bỏ Cascade
        customerRepository.delete(customer);
    }
}