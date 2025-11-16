package com.minh.springelectrostore.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minh.springelectrostore.user.dto.request.UpdateCustomerRequest;
import com.minh.springelectrostore.user.dto.response.CustomerResponse;
import com.minh.springelectrostore.user.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALE_MANAGER')") // Chỉ admin hoặc quản lý sale được xem
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
        Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALE_MANAGER')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable("id") Integer id) { // <--- THÊM ("id")
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomerInfo(
            @PathVariable("id") Integer id, // <--- THÊM ("id")
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse updatedCustomer = customerService.updateCustomerInfo(id, request);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") Integer id) { // <--- THÊM ("id")
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
