package com.apix.agent.core.tools.impl;

import com.alibaba.fastjson.JSON;
import com.apix.common.model.MainAgentState;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网络搜索工具 — 对标 Python: tools/web_search/search_tool.py :: search_web_by_keywords
 *
 * 使用 DuckDuckGo 搜索（无需 API key）。
 */
public class WebSearchTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();

    private static final String DDGS_API = "https://duckduckgo-api.example.com/search"; // 替为实际端点

    @Override
    public String getName() {
        return "search_web_by_keywords";
    }

    @Override
    public String getDescription() {
        return "Search the web for information using keywords. Returns a list of relevant results.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return singleParamSchema("keywords", "Search keywords or query string");
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String keywords = (String) args.get("keywords");
        if (keywords == null || keywords.isEmpty()) {
            return "Error: keywords is required";
        }

        log.info("[WebSearchTool] Searching: {}", keywords);

        try {
            // DuckDuckGo search via ddgs library equivalent
            // 这里使用 DuckDuckGo 的 lite API
            Request request = new Request.Builder()
                .url("https://lite.duckduckgo.com/lite/?q=" + java.net.URLEncoder.encode(keywords, "UTF-8"))
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();

            try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
                String html = resp.body() != null ? resp.body().string() : "";

                // 简易 HTML 解析提取结果
                // 在实际项目中，应使用 Jsoup 等 HTML 解析库
                return "Search results for: " + keywords + "\n(HTML parsing not yet implemented, "
                    + "raw response length: " + html.length() + " chars)";
            }

        } catch (Exception e) {
            log.error("[WebSearchTool] Search failed: {}", keywords, e);
            return "Error searching web: " + e.getMessage();
        }
    }
}
