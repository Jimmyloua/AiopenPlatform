package com.aiplatform.platform.service;

import com.aiplatform.platform.dto.*;
import com.aiplatform.platform.model.User;
import com.aiplatform.platform.repository.UserRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * User Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Register a new user
     */
    public UserResponse register(UserRegisterRequest request) {
        // Check if username exists
        if (userRepository.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (userRepository.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())) > 0) {
            throw new RuntimeException("Email already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("user");
        user.setStatus("active");

        userRepository.insert(user);

        return toUserResponse(user);
    }

    /**
     * Login user
     */
    public AuthResponse login(UserLoginRequest request) {
        // Find user by username or email
        User user = userRepository.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getEmail, request.getUsername()));

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        if (!"active".equals(user.getStatus())) {
            throw new RuntimeException("User account is not active");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user);
        Long expiresIn = jwtService.getExpirationTime();

        return new AuthResponse(token, expiresIn, toUserResponse(user));
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return toUserResponse(user);
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    /**
     * Convert to UserResponse
     */
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

}