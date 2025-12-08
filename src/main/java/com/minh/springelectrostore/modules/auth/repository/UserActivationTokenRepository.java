package com.minh.springelectrostore.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minh.springelectrostore.modules.auth.entity.UserActivationToken;

import java.util.Optional;

@Repository
public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {

    /**
     * Tìm token bằng chuỗi token.
     * @param token Chuỗi token duy nhất.
     * @return Optional chứa UserActivationToken nếu tìm thấy.
     */
    Optional<UserActivationToken> findByToken(String token);
}