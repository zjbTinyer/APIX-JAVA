package com.apix.memory.controller;

import com.apix.common.model.R;
import com.apix.memory.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理 API — 对标 Python: routers/user_record.py
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return R.fail("Username and password are required");
        }

        Map<String, Object> result = userService.login(username, password);
        if ((boolean) result.get("success")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) result.get("messages");
            return R.ok(messages);
        } else {
            return R.fail((String) result.get("message"));
        }
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return R.fail("Username and password are required");
        }

        Map<String, Object> result = userService.register(username, password);
        if ((boolean) result.get("success")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) result.get("messages");
            return R.ok(messages);
        } else {
            return R.fail((String) result.get("message"));
        }
    }

    /**
     * 验证用户 (用于恢复登录状态)。
     */
    @PostMapping("/ensure")
    public R<Map<String, Object>> ensure(@RequestBody Map<String, String> body) {
        String userUid = body.get("user_uid");
        if (userUid == null || userUid.isEmpty()) {
            return R.fail("user_uid is required");
        }

        Map<String, Object> result = userService.ensureUser(userUid);
        if ((boolean) result.get("success")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> messages = (Map<String, Object>) result.get("messages");
            return R.ok(messages);
        } else {
            return R.fail((String) result.get("message"));
        }
    }
}
