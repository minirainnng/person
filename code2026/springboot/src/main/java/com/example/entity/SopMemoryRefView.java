package com.example.entity;

import java.util.Date;
import java.util.List;

/**
 * 用于编辑器侧栏展示的引用 DTO：包含收藏当前是否存在
 * （如果收藏被删，memoryId 为空，仅展示标题快照）
 */
public class SopMemoryRefView {

    private Long id;
    private Long sopId;
    private Long memoryId;
    private String memoryTitleSnapshot;
    private Boolean memoryAlive;
    private String memoryTitle;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSopId() { return sopId; }
    public void setSopId(Long sopId) { this.sopId = sopId; }

    public Long getMemoryId() { return memoryId; }
    public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }

    public String getMemoryTitleSnapshot() { return memoryTitleSnapshot; }
    public void setMemoryTitleSnapshot(String memoryTitleSnapshot) { this.memoryTitleSnapshot = memoryTitleSnapshot; }

    public Boolean getMemoryAlive() { return memoryAlive; }
    public void setMemoryAlive(Boolean memoryAlive) { this.memoryAlive = memoryAlive; }

    public String getMemoryTitle() { return memoryTitle; }
    public void setMemoryTitle(String memoryTitle) { this.memoryTitle = memoryTitle; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
