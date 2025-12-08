package com.minh.springelectrostore.modules.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
    private String description;
    // Tên vai trò (name) thường không cho phép cập nhật
}
