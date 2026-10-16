package com.example.controller;

import com.example.common.Result;
import com.example.entity.Tag;
import com.example.service.TagService;
import com.example.ai.AiTagService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
public class TagController {
    @Resource private TagService service;
    @Resource private AiTagService aiTagService;

    @GetMapping
    public Result all() { return Result.success(service.all()); }

    @PostMapping
    public Result create(@RequestBody Map<String, String> body) {
        return Result.success(service.create(body.get("name")));
    }

    @PutMapping("/{id}")
    public Result rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.rename(id, body.get("name"));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) { service.delete(id); return Result.success(); }

    @GetMapping("/memories/{memoryId}")
    public Result forMemory(@PathVariable Long memoryId) { return Result.success(service.forMemory(memoryId)); }

    @PutMapping("/memories/{memoryId}")
    public Result replace(@PathVariable Long memoryId, @RequestBody List<Long> tagIds) {
        service.replace(memoryId, tagIds);
        return Result.success();
    }

    @GetMapping("/search")
    public Result search(@RequestParam List<Long> tagIds, @RequestParam(defaultValue = "true") boolean matchAll) {
        return Result.success(service.search(tagIds, matchAll));
    }

    // AI 从备注提取建议标签的接口预留；尚未配置模型，不提供假结果。
    @PostMapping("/suggest/{memoryId}")
    public Result suggest(@PathVariable Long memoryId) {
        return Result.success(aiTagService.suggestTags(memoryId));
    }
}
