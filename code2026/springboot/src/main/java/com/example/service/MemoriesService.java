package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Memories;
import com.example.exception.CustomException;
import com.example.mapper.MemoriesMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务处理
 **/
@Service
public class MemoriesService {

    @Resource
    private MemoriesMapper memoriesMapper;

    /**
     * 新增
     */
    public void add(Memories memories) {
        memoriesMapper.insert(memories);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        memoriesMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(Memories memories) {
        memoriesMapper.updateById(memories);
    }

    /**
     * 根据ID查询
     */
    public Memories selectById(Long id) {
        return memoriesMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Memories> selectAll(Memories memories) {
        return memoriesMapper.selectAll(memories);
    }

    public List<Memories> selectByFolderId(Long folderId) {
        return memoriesMapper.selectByFolderId(folderId);
    }

    /**
     * 分页查询
     */
    public PageInfo<Memories> selectPage(Memories memories, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Memories> list = memoriesMapper.selectAll(memories);
        return PageInfo.of(list);
    }
}