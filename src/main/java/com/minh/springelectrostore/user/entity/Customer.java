package com.minh.springelectrostore.user.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    // SỬA LỖI Ở ĐÂY: Ánh xạ tường minh tới cột "full_name"
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullname;

    // SỬA LỖI Ở ĐÂY: Ánh xạ tường minh tới cột "phone_number"
    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "photo", length = 255)
    private String photo;
    
    @Size(max = 200, message = "Giới thiệu không được quá 200 ký tự")
    private String bio;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();
    
    @Column(length = 10)
    private String gender; // "MALE", "FEMALE", "OTHER"

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}