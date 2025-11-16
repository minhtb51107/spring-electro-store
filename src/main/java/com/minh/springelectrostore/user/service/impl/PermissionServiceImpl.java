package com.minh.springelectrostore.user.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.user.dto.request.CreatePermissionRequest;
import com.minh.springelectrostore.user.dto.request.UpdatePermissionRequest;
import com.minh.springelectrostore.user.dto.response.PermissionResponse;
import com.minh.springelectrostore.user.entity.Permission;
import com.minh.springelectrostore.user.mapper.PermissionMapper;
import com.minh.springelectrostore.user.repository.PermissionRepository;
import com.minh.springelectrostore.user.repository.RoleRepository;
import com.minh.springelectrostore.user.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service // Đánh dấu là một Service Bean
@RequiredArgsConstructor // Tự động inject các dependency final qua constructor
@Transactional // Mặc định các phương thức trong lớp này đều được quản lý trong một transaction
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final RoleRepository roleRepository; // <-- THÊM DEPENDENCY

    @Override
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        String normalizedName = request.getName().toUpperCase().trim();
        // 1. Kiểm tra xem tên quyền đã tồn tại chưa
        if (permissionRepository.findByName(normalizedName).isPresent()) {
            throw new BadRequestException("Tên quyền đã tồn tại: " + normalizedName);
        }

        // 2. Dùng mapper để chuyển đổi DTO thành Entity
        Permission newPermission = permissionMapper.toPermissionEntity(request);
        
        // 3. Lưu vào database
        Permission savedPermission = permissionRepository.save(newPermission);

        // 4. Dùng mapper để chuyển đổi Entity đã lưu thành DTO trả về
        return permissionMapper.toPermissionResponse(savedPermission);
    }

    @Override
    @Transactional(readOnly = true) // Tối ưu hóa cho các thao tác chỉ đọc
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionResponse updatePermission(Integer permissionId, UpdatePermissionRequest request) {
        // 1. Tìm quyền trong database
        Permission existingPermission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền với ID: " + permissionId));

        // 2. Cập nhật thông tin (chỉ mô tả)
        existingPermission.setDescription(request.getDescription());

        // 3. Lưu lại thay đổi
        Permission updatedPermission = permissionRepository.save(existingPermission);

        // 4. Trả về DTO
        return permissionMapper.toPermissionResponse(updatedPermission);
    }

    @Override
    public void deletePermission(Integer permissionId) {
        // 1. Kiểm tra quyền có tồn tại không
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException("Không tìm thấy quyền với ID: " + permissionId);
        }
        
        // 2. *** KIỂM TRA RÀNG BUỘC (CẢI TIẾN MỚI) ***
        // Kiểm tra xem quyền này có đang được gán cho vai trò nào không.
        long rolesCount = roleRepository.countByPermissions_Id(permissionId);
        if (rolesCount > 0) {
            throw new BadRequestException("Không thể xóa quyền này vì nó đang được sử dụng bởi " + rolesCount + " vai trò.");
        }

        // 3. Xóa quyền
        permissionRepository.deleteById(permissionId);
    }
}