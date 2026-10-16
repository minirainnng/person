package com.example.mapper;

import com.example.entity.Sops;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SopsMapper {

    int insert(Sops sops);

    int updateById(Sops sops);

    int deleteById(Long id);

    Sops selectById(Long id);

    /**
     * 列表查询：按 sortOrder 升序、updated_at 倒序
     */
    List<Sops> selectAll(@Param("keyword") String keyword);
}
