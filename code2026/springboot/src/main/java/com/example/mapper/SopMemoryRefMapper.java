package com.example.mapper;

import com.example.entity.SopMemoryRef;
import com.example.entity.SopMemoryRefView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SopMemoryRefMapper {

    /**
     * 引用是否存在：sopId + memoryId（用于去重）
     */
    SopMemoryRef findBySopAndMemory(@Param("sopId") Long sopId, @Param("memoryId") Long memoryId);

    /**
     * 已存在则更新快照（标题可能修改过），不存在则插入
     */
    int upsert(SopMemoryRef ref);

    /**
     * 列出某 SOP 下的全部引用（带收藏存活状态）
     */
    List<SopMemoryRefView> listBySopId(@Param("sopId") Long sopId);

    int deleteById(@Param("id") Long id);

    int deleteBySopId(@Param("sopId") Long sopId);
}
