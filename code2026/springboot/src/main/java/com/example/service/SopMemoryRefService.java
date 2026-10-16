package com.example.service;

import com.example.entity.Memories;
import com.example.entity.SopMemoryRef;
import com.example.entity.SopMemoryRefView;
import com.example.exception.CustomException;
import com.example.mapper.MemoriesMapper;
import com.example.mapper.SopMemoryRefMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SopMemoryRefService {

    @Resource
    private SopMemoryRefMapper refMapper;
    @Resource
    private MemoriesMapper memoriesMapper;

    /**
     * 建立引用。重复时仅刷新标题快照。
     * memoryId 为空代表"仅快照"（已删除的收藏），前端不允许此场景。
     */
    public SopMemoryRef createRef(Long sopId, Long memoryId) {
        if (sopId == null) throw new CustomException("SOP id 不能为空");
        if (memoryId == null) throw new CustomException("收藏 id 不能为空");
        Memories memory = memoriesMapper.selectById(memoryId);
        if (memory == null) throw new CustomException("收藏不存在");
        String title = memory.getTitle();
        if (title == null || title.isEmpty()) title = memory.getSourceTitle();
        if (title == null) title = "未命名收藏";

        SopMemoryRef existing = refMapper.findBySopAndMemory(sopId, memoryId);
        if (existing != null) {
            existing.setMemoryTitleSnapshot(title);
            refMapper.upsert(existing);
            return existing;
        }
        SopMemoryRef ref = new SopMemoryRef();
        ref.setSopId(sopId);
        ref.setMemoryId(memoryId);
        ref.setMemoryTitleSnapshot(title);
        refMapper.upsert(ref);
        return ref;
    }

    public List<SopMemoryRefView> listBySopId(Long sopId) {
        return refMapper.listBySopId(sopId);
    }

    public void deleteById(Long id) {
        refMapper.deleteById(id);
    }
}
