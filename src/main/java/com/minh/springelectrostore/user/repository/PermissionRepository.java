package com.minh.springelectrostore.user.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Thêm import này

import com.minh.springelectrostore.user.entity.Permission;

@Repository // Thêm annotation này
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    
    Optional<Permission> findByName(String name);
    
    // Thêm phương thức này
    Set<Permission> findByNameIn(Set<String> names);
}