package com.minh.springelectrostore.user.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.dto.request.UpdateProfileRequest;
import com.minh.springelectrostore.user.dto.response.ProfileResponse;
import com.minh.springelectrostore.user.entity.Customer;
import com.minh.springelectrostore.user.entity.Employee;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.repository.CustomerRepository;
import com.minh.springelectrostore.user.repository.EmployeeRepository;
import com.minh.springelectrostore.user.repository.UserRepository;
import com.minh.springelectrostore.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional // Đặt Transactional ở cấp class
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository; // Inject CustomerRepository
    private final EmployeeRepository employeeRepository; // Inject EmployeeRepository
    
    
    // (Helper - Đảm bảo bạn có hàm này, nếu không, thay bằng userRepository.findByEmail(...))
    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    @Transactional(readOnly = true) // Tối ưu cho đọc
    public ProfileResponse getUserProfile(String userEmail) {
        User user = findUserByEmail(userEmail);
        return mapUserToProfileResponse(user);
    }

    @Override
    public ProfileResponse updateUserProfile(String userEmail, UpdateProfileRequest request) {
        User user = findUserByEmail(userEmail); //

        if (user.getCustomer() != null) {
            Customer customer = user.getCustomer(); 
            customer.setFullname(request.getFullname());
            customer.setPhoto(request.getPhotoUrl()); //
            customer.setBio(request.getBio()); //
            
            // Chỉ cần save, không cần hứng biến trả về
            customerRepository.save(customer); //
            
            // KHÔNG cần dòng user.setCustomer(...) nữa
            
        } else if (user.getEmployee() != null) {
            Employee employee = user.getEmployee();
            employee.setFullname(request.getFullname());
            employeeRepository.save(employee); //
            // KHÔNG cần dòng user.setEmployee(...) nữa
        } else {
             throw new IllegalStateException("User không có thông tin Customer hoặc Employee.");
        }

        // *** FIX QUAN TRỌNG NHẤT LÀ Ở ĐÂY ***
        // Vứt bỏ đối tượng 'user' cũ đang bị cache
        // và tải lại một đối tượng 'user' mới hoàn toàn từ CSDL.
        User refreshedUser = findUserByEmail(userEmail);
        
        // Trả về đối tượng 'user' MỚI,
        // đảm bảo nó chứa Customer với photoUrl mới nhất
        return mapUserToProfileResponse(refreshedUser); // [Sửa từ]
    }

    // Helper method để tìm User
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    // Helper method để map User sang ProfileResponse
    private ProfileResponse mapUserToProfileResponse(User user) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .status(user.getStatus().name());

        if (user.getCustomer() != null) {
        	builder.fullname(user.getCustomer().getFullname())   
            .photoUrl(user.getCustomer().getPhoto()) //    
            .bio(user.getCustomer().getBio()) //
            .userType("CUSTOMER");
        } else if (user.getEmployee() != null) {
            builder.fullname(user.getEmployee().getFullname())
                   // Employee hiện chưa có photoUrl
                   .userType("EMPLOYEE");
        } else {
            builder.fullname(user.getEmail()) // Fallback
                   .userType("UNKNOWN");
        }

        return builder.build();
    }
}