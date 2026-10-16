package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Folders;
import com.example.exception.CustomException;
import com.example.mapper.FoldersMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 业务处理
 **/
@Service
public class FoldersService {

    @Resource
    private FoldersMapper foldersMapper;

    /**
     * 新增
     */
    public void add(Folders folders) {

        Date date =new Date();
        String now = DateUtil.now();//2000-10-10 15：20：00

        foldersMapper.insert(folders);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        foldersMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(Folders folders) {
        foldersMapper.updateById(folders);
    }

    /**
     * 根据ID查询
     */
    public Folders selectById(Integer id) {
        return foldersMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Folders> selectAll(Folders folders) {
        return foldersMapper.selectAll(folders);
    }

    /**
     * 分页查询
     */
    public PageInfo<Folders> selectPage(Folders folders, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Folders> list = foldersMapper.selectAll(folders);
        return PageInfo.of(list);
    }
}