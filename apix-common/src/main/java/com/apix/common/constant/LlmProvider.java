package com.apix.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 供应商 URL 映射 — 对标 Python: global_config.py BASE_URL
 */
public class LlmProvider {

    private static final Map<String, String> BASE_URLS = new HashMap<>();

    static {
        BASE_URLS.put("ollama:local", "http://localhost:11434");
        BASE_URLS.put("ollama", "https://ollama.com");
        BASE_URLS.put("openai", "https://api.openai.com/v1");
        BASE_URLS.put("google", "https://generativelanguage.googleapis.com");
        BASE_URLS.put("qwen", "https://dashscope.aliyuncs.com/v1");
        BASE_URLS.put("qianfan", "https://qianfan.baidubce.com/v1");
        BASE_URLS.put("deepseek", "https://api.deepseek.com/v1");
        BASE_URLS.put("moonshot", "https://api.moonshot.cn/v1");
        BASE_URLS.put("xiaomimimo", "https://api.xiaomimimo.com/v1");
    }

    /**
     * 获取供应商的 base URL。
     * 自定义供应商通过 putCustomProvider 动态注册。
     */
    public static String getBaseUrl(String provider) {
        return BASE_URLS.get(provider);
    }

    /**
     * 注册自定义供应商 URL。
     */
    public static void putCustomProvider(String provider, String baseUrl) {
        BASE_URLS.put(provider, baseUrl);
    }

    /**
     * 判断是否为标准供应商。
     */
    public static boolean isStandardProvider(String provider) {
        return "ollama:local".equals(provider)
                || "ollama".equals(provider)
                || "openai".equals(provider)
                || "deepseek".equals(provider)
                || "moonshot".equals(provider)
                || "xiaomimimo".equals(provider)
                || "google".equals(provider)
                || "qwen".equals(provider)
                || "qianfan".equals(provider);
    }

    /**
     * 判断是否为自定义供应商（custom-开头的）。
     */
    public static boolean isCustomProvider(String provider) {
        return provider != null && provider.startsWith("custom-");
    }
}
