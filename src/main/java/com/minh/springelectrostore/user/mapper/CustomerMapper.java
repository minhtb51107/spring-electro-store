package com.minh.springelectrostore.user.mapper;

import org.springframework.stereotype.Component;

import com.minh.springelectrostore.auth.dto.request.RegisterRequest;
import com.minh.springelectrostore.user.dto.response.CustomerResponse;
import com.minh.springelectrostore.user.entity.Customer;


@Component // Đánh dấu đây là một Spring Bean
public class CustomerMapper {

    // Chuyển từ RegisterRequest DTO sang Customer Entity
    public Customer toCustomerEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        return Customer.builder()
                .fullname(request.getFullname())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    // Chuyển từ Customer Entity sang CustomerResponse DTO
    public CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullname(customer.getFullname())
                .phoneNumber(customer.getPhoneNumber())
                .photo(customer.getPhoto())
                .email(customer.getUser() != null ? customer.getUser().getEmail() : null)
                .status(customer.getUser() != null ? customer.getUser().getStatus().name() : null)
                .build();
    }
}