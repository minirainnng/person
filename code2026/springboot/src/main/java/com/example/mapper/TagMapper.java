package com.example.mapper;

import com.example.entity.Tag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagMapper {
    List<Tag> selectAll();
    Tag selectById(@Param("id") Long id);
    Tag selectByName(@Param("name") String name);
    int insert(Tag tag);
    int rename(@Param("id") Long id, @Param("name") String name);
    int deleteById(@Param("id") Long id);
    List<Tag> selectByMemoryId(@Param("memoryId") Long memoryId);
    List<Tag> selectByMemoryIds(@Param("memoryIds") List<Long> memoryIds);
    int deleteByMemoryId(@Param("memoryId") Long memoryId);
    int insertMemoryTag(@Param("memoryId") Long memoryId, @Param("tagId") Long tagId);
    List<Long> searchMemoryIds(@Param("tagIds") List<Long> tagIds, @Param("matchAll") boolean matchAll);
}
