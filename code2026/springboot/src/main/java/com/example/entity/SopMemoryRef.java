package com.example.entity;

import java.util.Date;

public class SopMemoryRef {

    /**主键 ID */
    private Long id;
    /**SOP 文档 ID */
    private Long sopId;
    /**收藏 ID（收藏被删除时置空） */
    private Long memoryId;
    /**建立引用时的收藏标题快照 */
    private String memoryTitleSnapshot;
    /**建立引用时间 */
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSopId() {
        return sopId;
    }

    public void setSopId(Long sopId) {
        this.sopId = sopId;
    }

    public Long getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Long memoryId) {
        this.memoryId = memoryId;
    }

    public String getMemoryTitleSnapshot() {
        return memoryTitleSnapshot;
    }

    public void setMemoryTitleSnapshot(String memoryTitleSnapshot) {
        this.memoryTitleSnapshot = memoryTitleSnapshot;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
