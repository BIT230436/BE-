package com.example.be.service;

import com.example.be.config.JwtUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.be.dto.LoginRequest;
import com.example.be.dto.RegisterRequest;
import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // In-memory store: token -> [email, expiryTimeMillis]
    private final Map<String, Object[]> passwordResetTokens = new ConcurrentHashMap<>();

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ==================== FORGOT PASSWORD ====================
    public Map<String, Object> forgotPassword(String email) {
        // Always return success to prevent email enumeration attacks
        if (userRepository.findByEmail(email).isEmpty()) {
            return Map.of("success", true, "message", "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.");
        }
        String token = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + 15L * 60 * 1000; // 15 phút
        passwordResetTokens.put(token, new Object[]{email, expiry});
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        try {
            emailService.sendPasswordResetEmail(email, resetLink);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Không thể gửi email. Vui lòng thử lại sau.");
        }
        return Map.of("success", true, "message", "Đường dẫn đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");
    }

    // ==================== RESET PASSWORD ====================
    public Map<String, Object> resetPassword(String token, String newPassword) {
        Object[] data = passwordResetTokens.get(token);
        if (data == null) {
            return Map.of("success", false, "message", "Token không hợp lệ hoặc đã được sử dụng.");
        }
        long expiry = (long) data[1];
        if (System.currentTimeMillis() > expiry) {
            passwordResetTokens.remove(token);
            return Map.of("success", false, "message", "Token đã hết hạn. Vui lòng gửi yêu cầu mới.");
        }
        String email = (String) data[0];
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokens.remove(token);
        return Map.of("success", true, "message", "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập với mật khẩu mới.");
    }

    // ==================== REGISTER ====================
    public Map<String, Object> register(RegisterRequest request) {
        try {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return Map.of("success", false, "message", "Email đã tồn tại!");
            }

            // Tạo user mới
            User user = new User();
            user.setEmail(request.getEmail());

            // ✅ FIX: Đảm bảo fullName không bị NULL
            String fullName = request.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                // Nếu không có fullName, tạo từ email
                String emailPrefix = request.getEmail().split("@")[0];
                fullName = emailPrefix.substring(0, 1).toUpperCase() + emailPrefix.substring(1);
            }
            user.setFullName(fullName);

            // ✅ Set username from email (required field)
            String username = request.getEmail().split("@")[0];
            String finalUsername = username;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = username + suffix;
                suffix++;
            }
            user.setUsername(finalUsername);

            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setStatus("ACTIVE"); //
            user.setAuthProvider("LOCAL"); // Set authProvider

            // Lấy role từ DB, luôn mặc định là USER khi đăng ký (tránh leo quyền từ client)
            String roleName = "USER";
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));
            user.setRole(role);

            System.out.println("🔍 DEBUG Register - Email: " + user.getEmail() +
                    " | Role: " + role.getName() +
                    " (ID: " + role.getId() + ") | Status: " + user.getStatus());

            User savedUser = userRepository.save(user);

            System.out.println("✅ User saved - ID: " + savedUser.getId() +
                    " | Role: " + savedUser.getRole().getName() +
                    " | Status: " + savedUser.getStatus());

            // Trả về format đồng nhất với login
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message",
                    "Đăng ký thành công! Tài khoản của bạn đang chờ admin duyệt. Bạn sẽ nhận được thông báo khi tài khoản được kích hoạt.");
            response.put("user", Map.of(
                    "id", savedUser.getId(),
                    "fullName", savedUser.getFullName(),
                    "email", savedUser.getEmail(),
                    "status", savedUser.getStatus(),
                    "role", savedUser.getRole().getName()));

            return response;

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Đăng ký thất bại: " + e.getMessage());
        }
    }

    // ==================== LOGIN (trả String đơn giản) ====================
    public String login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    if (!"ACTIVE".equals(user.getStatus())) {
                        return "Tài khoản chưa được duyệt!";
                    }
                    return "Đăng nhập thành công! Xin chào " + user.getEmail() // Sử dụng email thay vì username
                            + " (Role: " + user.getRole().getName() + ")";
                })
                .orElse("Sai email hoặc mật khẩu!");
    }

    // ==================== LOGIN (trả object cho frontend) ====================
    public Map<String, Object> loginWithResponse(LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email không tồn tại!");
            return response;
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("message", "Mật khẩu không đúng!");
            return response;
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            response.put("success", false);
            if ("PENDING".equals(user.getStatus())) {
                response.put("message", "Tài khoản đang chờ admin duyệt. Vui lòng liên hệ admin để được kích hoạt!");
            } else {
                response.put("message", "Tài khoản đã bị khóa hoặc vô hiệu hóa!");
            }
            return response;
        }

        response.put("success", true);
        response.put("message", "Đăng nhập thành công!");

        var permissions = user.getRole()
                .getPermissions()
                .stream()
                .map(p -> p.getName())
                .toList();
        response.put("user", Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "status", user.getStatus(),
                "role", user.getRole().getName(),
                "permissions", permissions));

        String token = jwtUtil.generateToken(request.getEmail(), user.getRole().getName());
        response.put("token", token);

        return response;
    }

    public User processOAuthPostLogin(org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
        String email = (String) oAuth2User.getAttribute("email");
        String name = (String) oAuth2User.getAttribute("name");
        String sub = (String) oAuth2User.getAttribute("sub");

        // Khai báo defaultRole trước để sử dụng chung
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        return userRepository.findByEmail(email)
                .map(user -> {
                    // Update thông tin cơ bản
                    user.setFullName(name);
                    user.setStatus("ACTIVE");
                    user.setAuthProvider("GOOGLE");

                    // Gán role USER nếu null hoặc không phải USER
                    if (user.getRole() == null) {
                        user.setRole(defaultRole);
                        System.out.println("🔍 Updated role to USER for existing user: " + email);
                    }

                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // Tạo mới user nếu chưa tồn tại
                    Role role = roleRepository.findByName("USER")
                            .orElseThrow(() -> new RuntimeException("Role mặc định không tồn tại"));

                    // Sinh dummy password (không dùng, nhưng bắt buộc để pass constraint)
                    String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());

                    User newUser = User.builder()
                            .email(email)
                            .username("google_" + sub) // tránh trùng username
                            .fullName(name)
                            .password(dummyPassword)
                            .role(defaultRole)
                            .authProvider("GOOGLE")
                            .status("ACTIVE")
                            .build();

                    System.out.println("🔍 Creating new user: Email: " + email +
                            " | Role: " + defaultRole.getName());

                    return userRepository.save(newUser);
                });
    }
}
