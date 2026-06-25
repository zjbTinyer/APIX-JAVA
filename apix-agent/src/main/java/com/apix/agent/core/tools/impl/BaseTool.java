package com.apix.agent.core.tools.impl;

import com.apix.agent.core.tools.ApixTool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具基类 — 提供公共方法。
 */
public abstract class BaseTool implements ApixTool {

    /**
     * 构建字符串参数 schema。
     */
    protected Map<String, Object> stringParam(String description, boolean required) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("type", "string");
        param.put("description", description);
        return param;
    }

    /**
     * 构建简单的参数 schema（只有一个 required 字段）。
     */
    protected Map<String, Object> singleParamSchema(String paramName, String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(paramName, stringParam(description, true));
        schema.put("properties", properties);

        schema.put("required", java.util.Collections.singletonList(paramName));
        return schema;
    }
}
