package com.minh.springelectrostore.modules.auth.request; // Chú ý package của bạn là 'request' hay 'dto.request' thì sửa lại cho đúng

import com.minh.springelectrostore.common.validation.FieldsValueMatch; // Import annotation mới tạo
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// --- ÁP DỤNG VALIDATION TẠI ĐÂY ---
@FieldsValueMatch.List({ 
    @FieldsValueMatch(
        field = "password", 
        fieldMatch = "confirmPassword", 
        message = "Mật khẩu nhập lại không khớp!"
    )
})
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullname;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;
    
    // Thêm trường này
    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
}