package com.example.service;

import com.example.entity.Tag;
import com.example.mapper.TagMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class TagService {
    @Resource private TagMapper mapper;

    public List<Tag> all() { return mapper.selectAll(); }
    public Tag create(String name) {
        String normalized = normalize(name);
        Tag existing = mapper.selectByName(normalized);
        if (existing != null) return existing;
        Tag tag = new Tag(); tag.setName(normalized); mapper.insert(tag); return tag;
    }
    public void rename(Long id, String name) {
        if (mapper.selectById(id) == null) throw new IllegalArgumentException("标签不存在");
        mapper.rename(id, normalize(name));
    }
    public void delete(Long id) { mapper.deleteById(id); }
    public List<Tag> forMemory(Long id) { return mapper.selectByMemoryId(id); }
    public List<Tag> forMemories(List<Long> ids) { return ids.isEmpty() ? List.of() : mapper.selectByMemoryIds(ids); }
    @Transactional
    public void replace(Long memoryId, List<Long> ids) {
        if (ids == null) throw new IllegalArgumentException("标签列表不能为空");
        Set<Long> unique = new LinkedHashSet<>(ids);
        for (Long id : unique) if (id == null || mapper.selectById(id) == null) throw new IllegalArgumentException("标签不存在: " + id);
        mapper.deleteByMemoryId(memoryId);
        for (Long id : unique) mapper.insertMemoryTag(memoryId, id);
    }
    public List<Long> search(List<Long> ids, boolean matchAll) {
        if (ids == null || ids.isEmpty()) return List.of();
        return mapper.searchMemoryIds(new ArrayList<>(new LinkedHashSet<>(ids)), matchAll);
    }
    private String normalize(String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 100) throw new IllegalArgumentException("标签名称须为1到100字");
        return name.trim();
    }
}
