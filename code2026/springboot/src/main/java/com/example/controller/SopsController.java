package com.example.controller;

import com.example.common.Result;
import com.example.entity.SopMemoryRef;
import com.example.entity.SopMemoryRefView;
import com.example.entity.Sops;
import com.example.service.SopMemoryRefService;
import com.example.service.SopsService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sops")
public class SopsController {

    @Resource
    private SopsService sopsService;
    @Resource
    private SopMemoryRefService refService;

    @GetMapping("/selectAll")
    public Result selectAll(@RequestParam(required = false) String keyword) {
        return Result.success(sopsService.selectAll(keyword));
    }

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "20") Integer pageSize) {
        PageInfo<Sops> page = sopsService.selectPage(keyword, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Long id) {
        return Result.success(sopsService.selectById(id));
    }

    @PostMapping("/add")
    public Result add(@RequestBody Sops sops) {
        sopsService.add(sops);
        return Result.success(sops.getId());
    }

    @PutMapping("/update")
    public Result update(@RequestBody Sops sops) {
        sopsService.updateById(sops);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        sopsService.deleteById(id);
        return Result.success();
    }

    // ============ 引用关系 ============

    @GetMapping("/{sopId}/refs")
    public Result listRefs(@PathVariable Long sopId) {
        List<SopMemoryRefView> list = refService.listBySopId(sopId);
        return Result.success(list);
    }

    @PostMapping("/{sopId}/refs/{memoryId}")
    public Result createRef(@PathVariable Long sopId, @PathVariable Long memoryId) {
        SopMemoryRef ref = refService.createRef(sopId, memoryId);
        return Result.success(ref);
    }

    @DeleteMapping("/refs/{refId}")
    public Result deleteRef(@PathVariable Long refId) {
        refService.deleteById(refId);
        return Result.success();
    }
}
