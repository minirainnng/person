package com.example.service;

import com.example.entity.Sops;
import com.example.exception.CustomException;
import com.example.mapper.SopsMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SopsService {

    @Resource
    private SopsMapper sopsMapper;

    public void add(Sops sops) {
        sops.setContentText(stripHtml(sops.getContentHtml()));
        sopsMapper.insert(sops);
    }

    public void updateById(Sops sops) {
        if (sops.getId() == null) throw new CustomException("SOP id 不能为空");
        sops.setContentText(stripHtml(sops.getContentHtml()));
        sopsMapper.updateById(sops);
    }

    public void deleteById(Long id) {
        sopsMapper.deleteById(id);
    }

    public Sops selectById(Long id) {
        return sopsMapper.selectById(id);
    }

    public List<Sops> selectAll(String keyword) {
        return sopsMapper.selectAll(keyword);
    }

    public PageInfo<Sops> selectPage(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(sopsMapper.selectAll(keyword));
    }

    /**简单去标签，用于搜索字段。保留纯文本换行。*/
    public static String stripHtml(String html) {
        if (html == null) return null;
        String text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
                   .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
        return text.replaceAll("\\s+", " ").trim();
    }
}
