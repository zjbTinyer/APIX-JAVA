package com.apix.common.model;

/**
 * 记忆条目。
 * 对标 Python: MemoItem
 */
public class MemoItem {

    private String title;
    private String date; // yyyy-MM-dd
    private String content;
    private String source; // "conversation" | "workspace"

    public MemoItem() {
    }

    public MemoItem(String title, String date, String content, String source) {
        this.title = title;
        this.date = date;
        this.content = content;
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
