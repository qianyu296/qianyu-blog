package com.study.qianyu_blog.service;

import com.study.qianyu_blog.dto.LoginRequest;
import com.study.qianyu_blog.dto.LoginResponse;
import com.study.qianyu_blog.entity.AdminUser;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.AdminUserRepository;
import com.study.qianyu_blog.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements CommandLineRunner {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${qianyu.admin.username}") String adminUsername,
            @Value("${qianyu.admin.password}") String adminPassword
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(40100, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(40100, "用户名或密码错误");
        }
        return new LoginResponse(jwtService.generateToken(user.getUsername()), "Bearer", jwtService.getExpirationMinutes());
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.existsByUsername(adminUsername)) {
            return;
        }
        AdminUser user = new AdminUser();
        user.setUsername(adminUsername);
        user.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUserRepository.save(user);
    }
}
