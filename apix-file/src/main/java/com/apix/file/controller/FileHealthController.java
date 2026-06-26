package com.apix.file.controller;

import com.apix.common.model.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FileHealthController {

    @GetMapping("/file/health")
    public R<Map<String, String>> health() {
        return R.ok(Map.of("status", "ok", "service", "file-service"));
    }
}
