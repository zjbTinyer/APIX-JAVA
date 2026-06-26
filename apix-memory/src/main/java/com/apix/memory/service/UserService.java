package com.apix.memory.service;

import com.apix.memory.entity.User;
import com.apix.memory.mapper.UserMapper;
import com.apix.memory.config.JwtConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务 — 登录、注册、认证。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 用户注册。
     */
    public Map<String, Object> register(String username, String password) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            result.put("success", false);
            result.put("message", "Username already exists");
            return result;
        }

        // 创建用户
        User user = new User();
        user.setUserUid(UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(username);
        user.setPassword(hashPassword(password));

        userMapper.insert(user);

        log.info("[UserService] Registered user: {}", username);

        result.put("success", true);
        result.put("messages", Map.of("uid", user.getUserUid()));
        return result;
    }

    /**
     * 用户登录。
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new LinkedHashMap<>();

        User user = userMapper.findByUsername(username);
        if (user == null || !checkPassword(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "Invalid username or password");
            return result;
        }

        // 生成 JWT
        String token = jwtConfig.generateToken(user.getUserUid(), username);

        log.info("[UserService] User logged in: {}", username);

        result.put("success", true);
        result.put("messages", Map.of(
                "uid", user.getUserUid(),
                "token", token));
        return result;
    }

    /**
     * 验证用户是否存在（用于恢复登录状态）。
     */
    public Map<String, Object> ensureUser(String userUid) {
        Map<String, Object> result = new LinkedHashMap<>();

        User user = userMapper.findByUserUid(userUid);
        if (user == null) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }

        result.put("success", true);
        result.put("messages", Map.of("uid", user.getUserUid(), "username", user.getUsername()));
        return result;
    }

    // ==================== 密码处理 ====================

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 使用 bcrypt 哈希密码。
     */
    private String hashPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    /**
     * 验证密码。
     */
    private boolean checkPassword(String rawPassword, String hashed) {
        return PASSWORD_ENCODER.matches(rawPassword, hashed);
    }
}
