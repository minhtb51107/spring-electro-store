package com.minh.springelectrostore.modules.user.service.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.modules.user.dto.request.AssignRolesToEmployeeRequest;
import com.minh.springelectrostore.modules.user.dto.request.CreateEmployeeRequest;
import com.minh.springelectrostore.modules.user.dto.request.UpdateEmployeeRequest;
import com.minh.springelectrostore.modules.user.dto.response.EmployeeResponse;
import com.minh.springelectrostore.modules.user.entity.Employee;
import com.minh.springelectrostore.modules.user.entity.Role;
import com.minh.springelectrostore.modules.user.entity.User;
import com.minh.springelectrostore.modules.user.entity.UserStatus;
import com.minh.springelectrostore.modules.user.mapper.EmployeeMapper;
import com.minh.springelectrostore.modules.user.repository.EmployeeRepository;
import com.minh.springelectrostore.modules.user.repository.RoleRepository;
import com.minh.springelectrostore.modules.user.repository.UserRepository;
import com.minh.springelectrostore.modules.user.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        // 1. Validate
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng.");
        }
        
        // 2. Tìm các Role từ CSDL
        Set<Role> roles = request.getRoleNames().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ: " + roleName)))
                .collect(Collectors.toSet());

        // 3. Tạo User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullname(request.getFullname()); // Đã thêm fullname để fix lỗi null
        
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        
        // [FIX] Vì User.java khai báo authProvider là String, ta set chuỗi trực tiếp
        user.setAuthProvider("LOCAL"); 

        // 4. Tạo Employee
        Employee employee = employeeMapper.toEmployeeEntity(request);
        
        // 5. Thiết lập quan hệ 2 chiều
        employee.setUser(user);
        employee.setRoles(roles);
        user.setEmployee(employee);

        // 6. Lưu User (Cascade sẽ lưu luôn Employee)
        User savedUser = userRepository.save(user);

        // 7. Trả về response
        return employeeMapper.toEmployeeResponse(savedUser.getEmployee());
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Integer id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toEmployeeResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(employeeMapper::toEmployeeResponse);
    }

    @Override
    public EmployeeResponse updateEmployee(Integer id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + id));

        // Cập nhật thông tin Employee
        if (request.getFullname() != null) {
            employee.setFullname(request.getFullname());
            // Đồng bộ cập nhật fullname sang User
            if (employee.getUser() != null) {
                employee.getUser().setFullname(request.getFullname());
            }
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }
        
        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.toEmployeeResponse(updatedEmployee);
    }

    @Override
    public void updateEmployeeStatus(Integer id, boolean isActive) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + id));
        
        employee.setActive(isActive);
        
        // [FIX] Sử dụng SUSPENDED thay vì INACTIVE vì Enum UserStatus không có INACTIVE
        if (employee.getUser() != null) {
            employee.getUser().setStatus(isActive ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        }
        
        employeeRepository.save(employee);
    }
    
    @Override
    public EmployeeResponse assignRolesToEmployee(Integer employeeId, AssignRolesToEmployeeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + employeeId));

        Set<Role> roles = request.getRoleNames().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new BadRequestException("Tên vai trò không hợp lệ: " + roleName)))
                .collect(Collectors.toSet());

        employee.setRoles(roles);
        Employee updatedEmployee = employeeRepository.save(employee);

        return employeeMapper.toEmployeeResponse(updatedEmployee);
    }
}