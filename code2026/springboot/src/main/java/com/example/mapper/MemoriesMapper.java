package com.example.mapper;

import com.example.entity.Memories;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作memories相关数据接口
*/
public interface MemoriesMapper {

    /**
      * 新增
    */
    int insert(Memories memories);

    /**
      * 删除
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Memories memories);

    /**
      * 根据ID查询
    */
    Memories selectById(Long id);

    /**
      * 查询所有
    */
    List<Memories> selectAll(Memories memories);

    List<Memories> selectByFolderId(Long folderId);

}