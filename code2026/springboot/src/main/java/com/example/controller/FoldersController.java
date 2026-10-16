package com.example.controller;

import com.example.common.Result;
import com.example.entity.Folders;
import com.example.service.FoldersService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员前端操作接口
 **/
@RestController
@RequestMapping("/folders")
public class FoldersController {

    @Resource
    private FoldersService foldersService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Folders folders) {
        foldersService.add(folders);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        foldersService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Folders folders) {
        foldersService.updateById(folders);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Folders folders = foldersService.selectById(id);
        return Result.success(folders);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Folders folders) {
        List<Folders> list = foldersService.selectAll(folders);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Folders folders,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "5") Integer pageSize) {
        PageInfo<Folders> page = foldersService.selectPage(folders, pageNum, pageSize);
        return Result.success(page);
    }

}