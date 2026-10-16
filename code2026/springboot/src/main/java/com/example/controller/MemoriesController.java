package com.example.controller;

import com.example.common.Result;
import com.example.entity.Memories;
import com.example.service.MemoriesService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员前端操作接口
 **/
@RestController
@RequestMapping("/memories")
public class MemoriesController {

    @Resource
    private MemoriesService memoriesService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Memories memories) {
        memoriesService.add(memories);
        return Result.success(memories.getId());
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        memoriesService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Memories memories) {
        memoriesService.updateById(memories);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Long id) {
        Memories memories = memoriesService.selectById(id);
        return Result.success(memories);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Memories memories, @RequestParam(required = false) Long folderId) {
        List<Memories> list = folderId == null
                ? memoriesService.selectAll(memories)
                : memoriesService.selectByFolderId(folderId);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Memories memories,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "5") Integer pageSize) {
        PageInfo<Memories> page = memoriesService.selectPage(memories, pageNum, pageSize);
        return Result.success(page);
    }

}