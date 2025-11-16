package com.minh.springelectrostore.auth.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.minh.springelectrostore.auth.dto.request.ChangePasswordRequest;
import com.minh.springelectrostore.auth.dto.request.ForgotPasswordRequest;
import com.minh.springelectrostore.auth.dto.request.LoginRequest;
import com.minh.springelectrostore.auth.dto.request.RegisterRequest;
import com.minh.springelectrostore.auth.dto.request.ResetPasswordRequest;
import com.minh.springelectrostore.auth.dto.response.JwtResponse;
import com.minh.springelectrostore.auth.entity.PasswordResetToken;
import com.minh.springelectrostore.auth.entity.UserActivationToken;
import com.minh.springelectrostore.auth.entity.UserSession;
import com.minh.springelectrostore.auth.repository.PasswordResetTokenRepository;
import com.minh.springelectrostore.auth.repository.UserActivationTokenRepository;
import com.minh.springelectrostore.auth.repository.UserSessionRepository;
import com.minh.springelectrostore.auth.service.AuthService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.service.EmailService;
import com.minh.springelectrostore.shared.util.JwtUtil;
import com.minh.springelectrostore.user.dto.response.UserDetailsResponse;
import com.minh.springelectrostore.user.entity.Customer;
import com.minh.springelectrostore.user.entity.User;
import com.minh.springelectrostore.user.entity.UserStatus;
import com.minh.springelectrostore.user.mapper.CustomerMapper;
import com.minh.springelectrostore.user.mapper.UserMapper;
import com.minh.springelectrostore.user.repository.CustomerRepository;
import com.minh.springelectrostore.user.repository.RoleRepository;
import com.minh.springelectrostore.user.repository.UserRepository;
import com.minh.springelectrostore.user.service.UserActivityLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Tự động inject các dependency final
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserSessionRepository userSessionRepository;
    private final UserActivityLogService userActivityLogService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserMapper userMapper;
    
    private final UserActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    @Value("${app.jwt.refresh-token-expiration-ms}") 
    private long refreshTokenExpirationMs;
    
    @Value("${app.security.max-concurrent-sessions}")
    private int maxConcurrentSessions;

    @Override
    public void registerCustomer(RegisterRequest request) {
        // 1. Validate dữ liệu
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng.");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Số điện thoại đã được sử dụng.");
        }

        // 2. Tạo User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullname(request.getFullname()); // <--- ĐÃ SỬA: Thêm dòng này
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        
        // 3. Tạo Customer
        Customer customer = customerMapper.toCustomerEntity(request);

        // 4. Thiết lập mối quan hệ hai chiều
        customer.setUser(user);
        user.setCustomer(customer);

        // 5. Lưu User (Cascade từ User sẽ tự động lưu Customer)
        userRepository.save(user);

        // 6. Tạo token kích hoạt và gửi email
        UserActivationToken activationToken = new UserActivationToken(user);
        activationTokenRepository.save(activationToken);

        String frontendUrl = "http://localhost:5173"; 
        String activationLink = frontendUrl + "/activate?token=" + activationToken.getToken();

        String emailBody = "<h1>Chào mừng bạn đến với MindRevol!</h1>" +
                           "<p>Vui lòng nhấp vào liên kết dưới đây để kích hoạt tài khoản của bạn:</p>" +
                           "<a href=\"" + activationLink + "\">Kích hoạt ngay</a>" +
                           "<p>Liên kết này sẽ hết hạn trong 24 giờ.</p>";
        emailService.sendEmail(user.getEmail(), "Kích hoạt tài khoản MindRevol", emailBody);
    }

    @Override
    public void activateUserAccount(String token) {
        UserActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token kích hoạt không hợp lệ."));

        if (activationToken.isExpired()) {
            activationTokenRepository.delete(activationToken);
            throw new BadRequestException("Token kích hoạt đã hết hạn.");
        }

        User user = activationToken.getUser();
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
             throw new BadRequestException("Tài khoản này đã được kích hoạt trước đó.");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        activationTokenRepository.delete(activationToken);
    }

    @Override
    public JwtResponse login(LoginRequest request, HttpServletRequest servletRequest) { 
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            String message;
            if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
                message = "Tài khoản của bạn chưa được kích hoạt. Vui lòng kiểm tra email.";
            } else { // UserStatus.SUSPENDED
                message = "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.";
            }
            throw new DisabledException(message);
        }

        long sessionCount = userSessionRepository.countByUserId(user.getId());
        if (sessionCount >= maxConcurrentSessions) {
            userSessionRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                    .ifPresent(userSessionRepository::delete);
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        String userAgent = servletRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(servletRequest);
        
        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .userAgent(userAgent) 
                .ipAddress(ipAddress)   
                .build();
        userSessionRepository.save(session);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
    
    @Override
    public JwtResponse refreshToken(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ hoặc đã bị thu hồi."));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            userSessionRepository.delete(session); 
            throw new BadRequestException("Refresh token đã hết hạn.");
        }

        User user = session.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userSessionRepository.save(session);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
    
    @Override
    public JwtResponse loginWithGoogle(String idTokenString, HttpServletRequest servletRequest) { 
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadRequestException("Token Google không hợp lệ.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewUserFromGoogle(payload));
            
            long sessionCount = userSessionRepository.countByUserId(user.getId());
            if (sessionCount >= maxConcurrentSessions) {
                userSessionRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                        .ifPresent(userSessionRepository::delete);
            }

            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            String userAgent = servletRequest.getHeader("User-Agent");
            String ipAddress = getClientIp(servletRequest);
            
            UserSession session = UserSession.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .expiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                    .userAgent(userAgent)
                    .ipAddress(ipAddress)
                    .build();
            userSessionRepository.save(session);
            
            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            throw new BadRequestException("Xác thực Google thất bại: " + e.getMessage());
        }
    }

    private User registerNewUserFromGoogle(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullname(name); // <--- ĐÃ SỬA: Thêm dòng này
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); 
        newUser.setStatus(UserStatus.ACTIVE); 
        
        newUser.setAuthProvider("GOOGLE");

        Customer newCustomer = new Customer();
        newCustomer.setFullname(name);
        newCustomer.setPhoto(pictureUrl);
        newCustomer.setUser(newUser);
        newUser.setCustomer(newCustomer); 

        userRepository.save(newUser); 
        
        return newUser;
    }
    
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            PasswordResetToken resetToken = new PasswordResetToken(user);
            passwordResetTokenRepository.save(resetToken);

            String frontendUrl = "http://localhost:5173"; 
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            
            String emailBody = "<h1>Yêu cầu đặt lại mật khẩu</h1>" +
                               "<p>Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>" +
                               "<p>Vui lòng nhấp vào liên kết dưới đây để đặt lại mật khẩu:</p>" +
                               "<a href=\"" + resetLink + "\">Đặt lại mật khẩu</a>" +
                               "<p>Liên kết này sẽ hết hạn trong 1 giờ. Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email này.</p>";
            emailService.sendEmail(user.getEmail(), "Yêu cầu đặt lại mật khẩu", emailBody);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Token đặt lại mật khẩu đã hết hạn.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        
        user.setAuthProvider("LOCAL"); 
        
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
    
    @Override
    public void changePassword(ChangePasswordRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng đã được xác thực."));

        if (!"LOCAL".equals(user.getAuthProvider())) {
            throw new BadRequestException("Tài khoản này không hỗ trợ đổi mật khẩu. Vui lòng sử dụng chức năng 'Tạo mật khẩu'.");
        }
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    @Override
    @Transactional(readOnly = true) 
    public UserDetailsResponse getCurrentUserDetails(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + userEmail));

        return userMapper.toUserDetailsResponse(user);
    }
    
    @Override
    public void logout(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ."));

        userSessionRepository.delete(session);
        
        userActivityLogService.logActivity("LOGOUT", null, session.getUser()); 
    }
}