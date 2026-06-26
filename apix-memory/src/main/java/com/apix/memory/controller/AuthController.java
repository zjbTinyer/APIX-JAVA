package com.apix.memory.controller;

import com.apix.memory.entity.User;
import com.apix.memory.mapper.UserMapper;
import com.apix.memory.config.JwtConfig;
import com.apix.memory.util.AesDecryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证 API — 对标 Electron 前端的登录/注册流程。
 *
 * 前端期望：
 * - 路径: POST /auth/login, /auth/register, /auth/ensure_user
 * - 密码: AES-128-CBC 加密后 base64
 * - 响应: { success: true, messages: { uid: "..." } }
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 登录 — POST /auth/login
     * Body: { "username": "xxx", "password": "<AES加密后的base64>" }
     * 成功: { "success": true, "messages": { "uid": "xxx", "token": "xxx" } }
     * 失败: { "success": false, "messages": "错误信息" }
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String username = body.get("username");
            String encryptedPwd = body.get("password");

            if (username == null || encryptedPwd == null) {
                result.put("success", false);
                result.put("messages", "Username and password are required");
                return result;
            }

            // AES 解密密码
            String password = AesDecryptUtil.decrypt(encryptedPwd);

            // 查找用户
            User user = userMapper.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("messages", "Invalid username or password");
                return result;
            }

            // 验证密码（简单哈希对比）
            if (!checkPassword(password, user.getPassword())) {
                result.put("success", false);
                result.put("messages", "Invalid username or password");
                return result;
            }

            // 生成 JWT
            String token = jwtConfig.generateToken(user.getUserUid(), username);

            log.info("[Auth] User logged in: {}", username);

            result.put("success", true);
            result.put("messages", Map.of(
                    "uid", user.getUserUid(),
                    "token", token));

        } catch (Exception e) {
            log.error("[Auth] Login error", e);
            result.put("success", false);
            result.put("messages", "Login failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * 注册 — POST /auth/register
     * Body: { "username": "xxx", "password": "<AES加密后的base64>" }
     * 成功: { "success": true, "messages": { "uid": "xxx", "token": "xxx" } }
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String username = body.get("username");
            String encryptedPwd = body.get("password");

            if (username == null || encryptedPwd == null) {
                result.put("success", false);
                result.put("messages", "Username and password are required");
                return result;
            }

            // AES 解密密码
            String password = AesDecryptUtil.decrypt(encryptedPwd);

            // 检查用户名是否已存在
            User existing = userMapper.findByUsername(username);
            if (existing != null) {
                result.put("success", false);
                result.put("messages", "Username already exists");
                return result;
            }

            // 创建用户
            User user = new User();
            user.setUserUid(UUID.randomUUID().toString().replace("-", ""));
            user.setUsername(username);
            user.setPassword(hashPassword(password));

            userMapper.insert(user);

            // 生成 JWT
            String token = jwtConfig.generateToken(user.getUserUid(), username);

            log.info("[Auth] User registered: {}", username);

            result.put("success", true);
            result.put("messages", Map.of(
                    "uid", user.getUserUid(),
                    "token", token));

        } catch (Exception e) {
            log.error("[Auth] Register error", e);
            result.put("success", false);
            result.put("messages", "Register failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * 校验用户 — POST /auth/ensure_user
     * Body: { "client_id": "user_uid" }
     * 成功: { "success": true, "messages": { "uid": "xxx" } }
     */
    @PostMapping("/ensure_user")
    public Map<String, Object> ensureUser(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String userUid = body.get("client_id");
            if (userUid == null || userUid.isEmpty()) {
                result.put("success", false);
                result.put("messages", "client_id is required");
                return result;
            }

            User user = userMapper.findByUserUid(userUid);
            if (user == null) {
                result.put("success", false);
                result.put("messages", "User not found");
                return result;
            }

            result.put("success", true);
            result.put("messages", Map.of("uid", user.getUserUid()));

        } catch (Exception e) {
            log.error("[Auth] Ensure user error", e);
            result.put("success", false);
            result.put("messages", "Ensure failed: " + e.getMessage());
        }

        return result;
    }

    // ==================== 密码工具 ====================
    // 委托给 UserService 的 BCryptPasswordEncoder，统一认证逻辑

    private String hashPassword(String password) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    private boolean checkPassword(String rawPassword, String hashed) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return encoder.matches(rawPassword, hashed);
    }
}
