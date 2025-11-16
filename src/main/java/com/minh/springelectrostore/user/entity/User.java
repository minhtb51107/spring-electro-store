package com.minh.springelectrostore.user.entity;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault; // <-- THÊM IMPORT NÀY

import com.minh.springelectrostore.auth.entity.UserSession;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "fullname", nullable = false, length = 100)
    private String fullname;
    
    @Column(name = "photo", length = 255)
    private String photo;

    @Builder.Default 
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING_ACTIVATION;
    
    // --- (PHẦN SỬA ĐỔI) ---
    @Column(name = "auth_provider", length = 20, nullable = false)
    @ColumnDefault("'LOCAL'") // <-- THÊM DÒNG NÀY (Lưu ý dấu ' bên trong)
    @Builder.Default
    private String authProvider = "LOCAL";
    // --- (KẾT THÚC SỬA ĐỔI) ---

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false, 
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // --- Relationships ---

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Customer customer;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee employee;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSession> sessions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserActivityLog> activityLogs;
}