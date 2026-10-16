package com.example.mapper;

import com.example.entity.Folders;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作folders相关数据接口
*/
public interface FoldersMapper {

    /**
      * 新增
    */
    int insert(Folders folders);

    /**
      * 删除
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Folders folders);

    /**
      * 根据ID查询
    */
    Folders selectById(Integer id);

    /**
      * 查询所有
    */
    List<Folders> selectAll(Folders folders);

}