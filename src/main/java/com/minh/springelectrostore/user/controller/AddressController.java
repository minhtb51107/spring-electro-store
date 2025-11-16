package com.minh.springelectrostore.user.controller;

import com.minh.springelectrostore.user.dto.request.AddressRequest;
import com.minh.springelectrostore.user.dto.response.AddressResponse;
import com.minh.springelectrostore.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses(Authentication authentication) {
        return ResponseEntity.ok(addressService.getMyAddresses(authentication.getName()));
    }
    
    @GetMapping("/default")
    public ResponseEntity<AddressResponse> getDefaultAddress(Authentication authentication) {
        return ResponseEntity.ok(addressService.getDefaultAddress(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request) {
        return new ResponseEntity<>(addressService.createAddress(authentication.getName(), request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {
        addressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}