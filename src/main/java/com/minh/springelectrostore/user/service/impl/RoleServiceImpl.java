package com.minh.springelectrostore.user.service.impl;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.dto.request.AssignPermissionToRoleRequest;
import com.minh.springelectrostore.user.dto.request.CreateRoleRequest;
import com.minh.springelectrostore.user.dto.response.RoleWithPermissionsResponse;
import com.minh.springelectrostore.user.entity.Permission;
import com.minh.springelectrostore.user.entity.Role;
import com.minh.springelectrostore.user.mapper.RoleMapper;
import com.minh.springelectrostore.user.repository.EmployeeRepository;
import com.minh.springelectrostore.user.repository.PermissionRepository;
import com.minh.springelectrostore.user.repository.RoleRepository;
import com.minh.springelectrostore.user.service.RoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final EmployeeRepository employeeRepository; // <-- THÊM DEPENDENCY

    @Override
    public RoleWithPermissionsResponse createRole(CreateRoleRequest request) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Tên vai trò đã tồn tại: " + request.getName());
        }
        Role newRole = new Role();
        newRole.setName(request.getName());
        newRole.setDescription(request.getDescription());
        
        Role savedRole = roleRepository.save(newRole);
        return roleMapper.toRoleWithPermissionsResponse(savedRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleWithPermissionsResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toRoleWithPermissionsResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoleWithPermissionsResponse assignPermissionsToRole(Integer roleId, AssignPermissionToRoleRequest request) {
        // 1. Tìm vai trò
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + roleId));

        // 2. Tìm tất cả các quyền hợp lệ từ CSDL
        Set<Permission> permissions = permissionRepository.findByNameIn(request.getPermissionNames());

        // 3. Kiểm tra xem có quyền nào không hợp lệ không
        if (permissions.size() != request.getPermissionNames().size()) {
            throw new BadRequestException("Một hoặc nhiều quyền không hợp lệ.");
        }

        // 4. Gán và lưu
        role.setPermissions(permissions);
        Role updatedRole = roleRepository.save(role);

        return roleMapper.toRoleWithPermissionsResponse(updatedRole);
    }
    
    @Override
    public void deleteRole(Integer roleId) {
        // 1. Kiểm tra vai trò có tồn tại không
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + roleId));
        
        // 2. *** KIỂM TRA RÀNG BUỘC (CẢI TIẾN MỚI) ***
        // Kiểm tra xem có nhân viên nào đang giữ vai trò này không
        long employeesCount = employeeRepository.countByRoles_Id(roleId);
        if (employeesCount > 0) {
            throw new BadRequestException("Không thể xóa vai trò '" + role.getName() + "' vì nó đang được gán cho " + employeesCount + " nhân viên.");
        }

        // 3. Xóa vai trò
        roleRepository.deleteById(roleId);
    }
}

