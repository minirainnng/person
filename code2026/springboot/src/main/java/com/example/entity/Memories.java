package com.example.entity;

import java.util.Date;

public class Memories {

    /**主键 ID */
    private long id;
    /**当前显示标题（可由用户自定义修改） */
    private String title;
    /**平台原始标题（网页/视频自带的标题） */
    private String sourceTitle;
    /**类型：video, web, image, text */
    private String sourceType;
    /**原始链接（可为空） */
    private String originalUrl;
    /**网页正文、图文提取内容或手动补充的文本 */
    private String originalText;
    /**图片存储路径（若为图片收藏） */
    private String imagePath;
    /**灵魂字段：用户自己写的主观备注 */
    private String note;
    /**创建时间 */
    private Date createdAt;
    /**更新时间 */
    private Date updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
