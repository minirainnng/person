package com.example.controller;

import com.example.common.Result;
import com.example.entity.MemoryFolders;
import com.example.service.MemoryFoldersService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员前端操作接口
 **/
@RestController
@RequestMapping("/memoryFolders")
public class MemoryFoldersController {

    @Resource
    private MemoryFoldersService memoryFoldersService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody MemoryFolders memoryFolders) {
        memoryFoldersService.add(memoryFolders);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        memoryFoldersService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/memory/{memoryId}")
    public Result replaceForMemory(@PathVariable String memoryId, @RequestBody List<String> folderIds) {
        memoryFoldersService.replaceForMemory(memoryId, folderIds);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody MemoryFolders memoryFolders) {
        memoryFoldersService.updateById(memoryFolders);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        MemoryFolders memoryFolders = memoryFoldersService.selectById(id);
        return Result.success(memoryFolders);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(MemoryFolders memoryFolders) {
        List<MemoryFolders> list = memoryFoldersService.selectAll(memoryFolders);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(MemoryFolders memoryFolders,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "5") Integer pageSize) {
        PageInfo<MemoryFolders> page = memoryFoldersService.selectPage(memoryFolders, pageNum, pageSize);
        return Result.success(page);
    }

}