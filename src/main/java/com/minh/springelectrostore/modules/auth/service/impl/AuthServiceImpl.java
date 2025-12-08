package com.minh.springelectrostore.modules.auth.service.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import com.minh.springelectrostore.common.exception.BadRequestException;
import com.minh.springelectrostore.common.exception.ResourceNotFoundException;
import com.minh.springelectrostore.common.service.EmailService;
import com.minh.springelectrostore.common.util.JwtUtil;
import com.minh.springelectrostore.modules.auth.entity.PasswordResetToken;
import com.minh.springelectrostore.modules.auth.entity.UserActivationToken;
import com.minh.springelectrostore.modules.auth.repository.PasswordResetTokenRepository;
import com.minh.springelectrostore.modules.auth.repository.UserActivationTokenRepository;
// import com.minh.springelectrostore.modules.auth.repository.UserSessionRepository; // Không dùng Repository này nữa
import com.minh.springelectrostore.modules.auth.request.ChangePasswordRequest;
import com.minh.springelectrostore.modules.auth.request.ForgotPasswordRequest;
import com.minh.springelectrostore.modules.auth.request.LoginRequest;
import com.minh.springelectrostore.modules.auth.request.RegisterRequest;
import com.minh.springelectrostore.modules.auth.request.ResetPasswordRequest;
import com.minh.springelectrostore.modules.auth.response.JwtResponse;
import com.minh.springelectrostore.modules.auth.service.AuthService;
import com.minh.springelectrostore.modules.user.dto.response.UserDetailsResponse;
import com.minh.springelectrostore.modules.user.entity.Customer;
import com.minh.springelectrostore.modules.user.entity.User;
import com.minh.springelectrostore.modules.user.entity.UserStatus;
import com.minh.springelectrostore.modules.user.mapper.CustomerMapper;
import com.minh.springelectrostore.modules.user.mapper.UserMapper;
import com.minh.springelectrostore.modules.user.repository.CustomerRepository;
import com.minh.springelectrostore.modules.user.repository.RoleRepository;
import com.minh.springelectrostore.modules.user.repository.UserRepository;
import com.minh.springelectrostore.modules.user.service.UserActivityLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Tự động inject các dependency final (bao gồm RedisTemplate)
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    
    // Đã loại bỏ UserSessionRepository vì chuyển sang dùng Redis
    // private final UserSessionRepository userSessionRepository; 
    
    private final UserActivityLogService userActivityLogService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserMapper userMapper;
    
    private final UserActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;
    
    // [QUAN TRỌNG] Inject RedisTemplate để thao tác với Redis
    private final RedisTemplate<String, Object> redisTemplate;
    
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
        user.setFullname(request.getFullname());
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
        // Lưu ý: Hàm này giờ đã có cơ chế Retry (nhờ @Retryable trong EmailWorker nếu bạn dùng Async)
        // Tuy nhiên ở đây đang gọi trực tiếp EmailService (Sync). 
        // Để tận dụng Async Retry, bạn nên gọi qua EmailWorker (cần inject EmailWorker thay vì EmailService).
        // Nhưng tạm thời giữ nguyên logic Sync này để tránh thay đổi quá nhiều file.
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

        // --- BỎ LOGIC CHECK SESSION TRONG DB ---
        /*
        long sessionCount = userSessionRepository.countByUserId(user.getId());
        if (sessionCount >= maxConcurrentSessions) {
            userSessionRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                    .ifPresent(userSessionRepository::delete);
        }
        */

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // --- THÊM LOGIC LƯU SESSION VÀO REDIS ---
        // Key: "auth:refresh_token:{token_string}" -> Value: userEmail
        String redisKey = "auth:refresh_token:" + refreshToken;
        redisTemplate.opsForValue().set(redisKey, user.getEmail(), refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        
        // Ghi log hoạt động (bất đồng bộ hoặc đơn giản)
        userActivityLogService.logActivity("LOGIN", "Đăng nhập hệ thống", user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Override
    public JwtResponse refreshToken(String refreshToken) {
        // --- LOGIC MỚI VỚI REDIS ---
        String redisKey = "auth:refresh_token:" + refreshToken;
        
        // 1. Kiểm tra token có tồn tại trong Redis không
        Object emailObj = redisTemplate.opsForValue().get(redisKey);
        
        if (emailObj == null) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }
        
        String userEmail = (String) emailObj;
        
        // 2. Xóa token cũ ngay lập tức (Token Rotation - Chống Replay Attack)
        redisTemplate.delete(redisKey);

        // 3. Lấy thông tin user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));

        // 4. Tạo cặp token mới
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        // 5. Lưu token mới vào Redis
        String newRedisKey = "auth:refresh_token:" + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, userEmail, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

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
            
            // Bỏ logic UserSessionRepository cũ
            
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            // Lưu vào Redis
            String redisKey = "auth:refresh_token:" + refreshToken;
            redisTemplate.opsForValue().set(redisKey, user.getEmail(), refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
            
            userActivityLogService.logActivity("LOGIN_GOOGLE", "Đăng nhập qua Google", user);
            
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
        newUser.setFullname(name);
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
        // --- LOGIC MỚI VỚI REDIS ---
        String redisKey = "auth:refresh_token:" + refreshToken;
        
        // Xóa key khỏi Redis -> Token hết hạn ngay lập tức
        Boolean deleted = redisTemplate.delete(redisKey);
        
        if (Boolean.FALSE.equals(deleted)) {
            // Có thể token đã hết hạn từ trước, log warning nhẹ nhàng
            // log.warn("Logout: Token không tồn tại hoặc đã hết hạn trong Redis.");
        }
        
        // Có thể lấy user từ token để log activity nếu cần (tuy nhiên logic lấy user từ redis trước khi xóa sẽ tốn thêm 1 query)
    }
}