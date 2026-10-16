package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.MemoryFolders;
import com.example.exception.CustomException;
import com.example.mapper.MemoryFoldersMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 业务处理
 **/
@Service
public class MemoryFoldersService {

    @Resource
    private MemoryFoldersMapper memoryFoldersMapper;

    /**
     * 新增
     */
    public void add(MemoryFolders memoryFolders) {
        if (memoryFolders.getCreatedAt() == null) {
            memoryFolders.setCreatedAt(new Date());
        }
        memoryFoldersMapper.insert(memoryFolders);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        memoryFoldersMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(MemoryFolders memoryFolders) {
        memoryFoldersMapper.updateById(memoryFolders);
    }

    /**
     * 根据ID查询
     */
    public MemoryFolders selectById(Integer id) {
        return memoryFoldersMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<MemoryFolders> selectAll(MemoryFolders memoryFolders) {
        return memoryFoldersMapper.selectAll(memoryFolders);
    }

    @Transactional
    public void replaceForMemory(String memoryId, List<String> folderIds) {
        memoryFoldersMapper.deleteByMemoryId(memoryId);
        if (folderIds == null) return;
        for (String folderId : folderIds) {
            MemoryFolders relation = new MemoryFolders();
            relation.setMemoryId(memoryId);
            relation.setFolderId(folderId);
            relation.setCreatedAt(new Date());
            memoryFoldersMapper.insert(relation);
        }
    }

    /**
     * 分页查询
     */
    public PageInfo<MemoryFolders> selectPage(MemoryFolders memoryFolders, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<MemoryFolders> list = memoryFoldersMapper.selectAll(memoryFolders);
        return PageInfo.of(list);
    }
}