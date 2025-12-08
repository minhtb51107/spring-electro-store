package com.minh.springelectrostore.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class FieldsValueMatchValidator implements ConstraintValidator<FieldsValueMatch, Object> {

    private String field;
    private String fieldMatch;

    @Override
    public void initialize(FieldsValueMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // Dùng Reflection lấy giá trị của 2 trường từ object DTO
            Object fieldValue = new BeanWrapperImpl(value).getPropertyValue(field);
            Object fieldMatchValue = new BeanWrapperImpl(value).getPropertyValue(fieldMatch);

            // So sánh
            if (fieldValue != null) {
                return fieldValue.equals(fieldMatchValue);
            } else {
                return fieldMatchValue == null;
            }
        } catch (Exception e) {
            // Nếu có lỗi (ví dụ sai tên trường), coi như không hợp lệ
            return false;
        }
    }
}