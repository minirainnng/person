package com.example.entity;

import java.util.Date;

public class Folders {

    /**主键 ID */
    private String id;
    /**父文件夹 ID（根目录通常为 NULL 或 0） */
    private String parentId;
    /**文件夹名称（如：“眼珠配色”、“视频剪辑”） */
    private String name;
    /**排序权重 */
    private Integer sortOrder;
    /**创建时间 */
    private Date createdAt;
    /**更新时间 */
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
