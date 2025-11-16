package com.minh.springelectrostore.config.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.springelectrostore.user.entity.Employee;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // Lấy quyền (authorities) từ roles
        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                true, true, true, true, // Các cờ enabled, accountNonExpired, etc.
                authorities
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Employee employee = user.getEmployee();

        // Nếu là nhân viên, lấy role từ employee
        if (employee != null && employee.getRoles() != null) {
            employee.getRoles().forEach(role -> {
                // 1. Thêm Role (Ví dụ: ROLE_ADMIN)
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                
                // 2. --- FIX: BỎ COMMENT ĐOẠN NÀY ĐỂ NẠP QUYỀN ---
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        // Nạp quyền (Ví dụ: VIEW_ALL_ORDERS)
                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                    });
                }
                // ------------------------------------------------
            });
        }
        // Nếu là khách hàng
        else {
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }

        return authorities;
    }
}