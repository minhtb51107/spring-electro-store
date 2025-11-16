package com.minh.springelectrostore.user.mapper;

import com.minh.springelectrostore.user.dto.request.AddressRequest;
import com.minh.springelectrostore.user.dto.response.AddressResponse;
import com.minh.springelectrostore.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address toEntity(AddressRequest request);
    AddressResponse toResponse(Address address);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    void updateEntity(AddressRequest request, @MappingTarget Address address);
}