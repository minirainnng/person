package com.example.mapper;

import com.example.entity.MemoryFolders;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作memoryFolders相关数据接口
*/
public interface MemoryFoldersMapper {

    /**
      * 新增
    */
    int insert(MemoryFolders memoryFolders);

    /**
      * 删除
    */
    int deleteById(Integer id);

    int deleteByMemoryId(String memoryId);

    /**
      * 修改
    */
    int updateById(MemoryFolders memoryFolders);

    /**
      * 根据ID查询
    */
    MemoryFolders selectById(Integer id);

    /**
      * 查询所有
    */
    List<MemoryFolders> selectAll(MemoryFolders memoryFolders);

}