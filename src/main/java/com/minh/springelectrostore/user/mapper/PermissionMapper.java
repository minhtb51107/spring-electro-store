package com.minh.springelectrostore.user.mapper;

import org.springframework.stereotype.Component;

import com.minh.springelectrostore.user.dto.request.CreatePermissionRequest;
import com.minh.springelectrostore.user.dto.response.PermissionResponse;
import com.minh.springelectrostore.user.entity.Permission;

@Component // Đánh dấu là một Spring Bean để có thể inject vào các service khác
public class PermissionMapper {

    /**
     * Chuyển đổi từ Permission Entity sang PermissionResponse DTO.
     * Dùng để trả dữ liệu về cho client.
     *
     * @param permission Đối tượng Entity từ database.
     * @return Đối tượng DTO chứa thông tin an toàn để hiển thị.
     */
    public PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }

    /**
     * Chuyển đổi từ CreatePermissionRequest DTO sang Permission Entity.
     * Dùng khi tạo một quyền mới.
     *
     * @param request Dữ liệu từ client gửi lên.
     * @return Một đối tượng Entity mới sẵn sàng để lưu vào database.
     */
    public Permission toPermissionEntity(CreatePermissionRequest request) {
        if (request == null) {
            return null;
        }
        return Permission.builder()
                .name(request.getName().toUpperCase().trim()) // Chuẩn hóa tên quyền: viết hoa và bỏ khoảng trắng thừa
                .description(request.getDescription())
                .build();
    }
}