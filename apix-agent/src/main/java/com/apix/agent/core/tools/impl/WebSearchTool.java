package com.apix.agent.core.tools.impl;

import com.alibaba.fastjson.JSON;
import com.apix.common.model.MainAgentState;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网络搜索工具 — 使用 DuckDuckGo 搜索（无需 API Key）
 *
 * 通过解析 DuckDuckGo 的 HTML 结果页面提取搜索结果标题、链接和摘要。
 */
public class WebSearchTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    @Override
    public String getName() {
        return "search_web_by_keywords";
    }

    @Override
    public String getDescription() {
        return "Search the web for information using keywords. Returns a list of relevant results with titles, links and snippets.";
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
            // 1. 请求 DuckDuckGo HTML 搜索页面
            String encodedQuery = java.net.URLEncoder.encode(keywords, "UTF-8");
            Request request = new Request.Builder()
                    .url("https://html.duckduckgo.com/html/?q=" + encodedQuery)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build();

            try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
                String html = resp.body() != null ? resp.body().string() : "";

                // 2. 使用 Jsoup 解析 HTML 提取搜索结果
                Document doc = Jsoup.parse(html);
                Elements resultElements = doc.select(".result");

                List<Map<String, String>> results = new ArrayList<>();

                for (Element result : resultElements) {
                    Element titleEl = result.selectFirst(".result__title a");
                    Element snippetEl = result.selectFirst(".result__snippet");

                    if (titleEl == null)
                        continue;

                    String title = titleEl.text().trim();
                    String link = titleEl.attr("href");
                    // DuckDuckGo 用 rel="nofollow" 的链接包装实际 URL
                    if (link.startsWith("//")) {
                        link = "https:" + link;
                    }
                    String snippet = snippetEl != null ? snippetEl.text().trim() : "";

                    if (!title.isEmpty()) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("title", title);
                        item.put("link", link);
                        item.put("snippet", snippet);
                        results.add(item);
                    }
                }

                // 3. 限制返回数量，避免 Token 超限
                int maxResults = Math.min(results.size(), 10);
                results = results.subList(0, maxResults);

                log.info("[WebSearchTool] Found {} results for: {}", results.size(), keywords);

                Map<String, Object> output = new LinkedHashMap<>();
                output.put("query", keywords);
                output.put("total_results", results.size());
                output.put("results", results);

                return JSON.toJSONString(output);
            }

        } catch (Exception e) {
            log.error("[WebSearchTool] Search failed: {}", keywords, e);
            return "Error searching web: " + e.getMessage();
        }
    }
}
