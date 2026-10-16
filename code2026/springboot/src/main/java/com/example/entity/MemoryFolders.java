package com.example.entity;

import java.util.Date;

public class MemoryFolders {

    /**主键 ID */
    private String id;
    /**收藏表 ID (memories.id) */
    private String memoryId;
    /**文件夹表 ID (folders.id) */
    private String folderId;
    /**创建时间 */
    private java.util.Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public java.util.Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }
}
