package com.minh.springelectrostore.user.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.minh.springelectrostore.user.dto.request.CreateEmployeeRequest;
import com.minh.springelectrostore.user.dto.response.EmployeeResponse;
import com.minh.springelectrostore.user.entity.Employee;
import com.minh.springelectrostore.user.entity.Role;

@Component
public class EmployeeMapper {

    public Employee toEmployeeEntity(CreateEmployeeRequest request) {
        if (request == null) return null;
        return Employee.builder()
                .fullname(request.getFullname())
                .employeeCode(request.getEmployeeCode())
                .position(request.getPosition())
                .department(request.getDepartment())
                .hiredDate(request.getHiredDate())
                .build();
    }

    public EmployeeResponse toEmployeeResponse(Employee employee) {
        if (employee == null) return null;
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullname(employee.getFullname())
                .position(employee.getPosition())
                .department(employee.getDepartment())
                .hiredDate(employee.getHiredDate())
                .isActive(employee.isActive())
                .email(employee.getUser() != null ? employee.getUser().getEmail() : null)
                .status(employee.getUser() != null ? employee.getUser().getStatus().name() : null)
                .roleNames(employee.getRoles() != null ?
                        employee.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) :
                        Collections.emptySet())
                .build();
    }
}