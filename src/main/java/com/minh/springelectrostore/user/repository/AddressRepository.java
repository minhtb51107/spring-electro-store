package com.minh.springelectrostore.user.repository;

import com.minh.springelectrostore.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    // Tìm tất cả địa chỉ của khách hàng
    List<Address> findByCustomer_Id(Integer customerId);
    
    // Tìm địa chỉ đang là mặc định của khách hàng
    Optional<Address> findByCustomer_IdAndIsDefaultTrue(Integer customerId);

	long countByCustomerId(Integer id);
}