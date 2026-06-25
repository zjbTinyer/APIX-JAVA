package com.apix.common.model;

/**
 * 角色卡 — 用户自定义的助手身份。
 * 对标 Python: RoleSchema
 */
public class RoleSchema {

    /** 角色名（如"代码专家"） */
    private String name;

    /** 角色定义（系统提示词） */
    private String definition;

    public RoleSchema() {
    }

    public RoleSchema(String name, String definition) {
        this.name = name;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }
}
