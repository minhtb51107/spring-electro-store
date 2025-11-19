package com.minh.springelectrostore.promotion.controller;

import com.minh.springelectrostore.promotion.dto.request.VoucherRequest;
import com.minh.springelectrostore.promotion.dto.response.VoucherResponse;
import com.minh.springelectrostore.promotion.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // --- PUBLIC API (Cho khách hàng xem voucher) ---
    @GetMapping
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @PageableDefault(size = 10, sort = "endDate") Pageable pageable) {
        return ResponseEntity.ok(voucherService.getAllVouchers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    // --- ADMIN API (Quản lý) ---
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Hoặc hasAuthority('MANAGE_VOUCHERS')
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return new ResponseEntity<>(voucherService.createVoucher(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }
}