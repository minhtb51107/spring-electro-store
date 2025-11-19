package com.minh.springelectrostore.promotion.mapper;

import com.minh.springelectrostore.promotion.dto.request.VoucherRequest;
import com.minh.springelectrostore.promotion.dto.response.VoucherResponse;
import com.minh.springelectrostore.promotion.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    Voucher toEntity(VoucherRequest request);

    VoucherResponse toResponse(Voucher voucher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedCount", ignore = true) // Không cho update số lần đã dùng qua API này
    void updateEntity(VoucherRequest request, @MappingTarget Voucher voucher);
}