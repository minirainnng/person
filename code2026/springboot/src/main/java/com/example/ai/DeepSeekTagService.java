package com.example.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.entity.Memories;
import com.example.mapper.MemoriesMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekTagService implements AiTagService {
    private final MemoriesMapper memoriesMapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public DeepSeekTagService(
            MemoriesMapper memoriesMapper,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.model:deepseek-chat}") String model,
            @Value("${ai.deepseek.enabled:false}") boolean enabled) {
        this.memoriesMapper = memoriesMapper;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://api.deepseek.com").build();
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    @Override
    public List<String> suggestTags(Long memoryId) {
        if (!enabled) throw new IllegalStateException("DeepSeek 标签分析未启用");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("未配置 DeepSeek API Key");
        Memories memory = memoriesMapper.selectById(memoryId);
        if (memory == null) throw new IllegalArgumentException("收藏不存在: " + memoryId);

        String userContent = "标题：\n" + safe(memory.getTitle())
                + "\n\n平台原始标题：\n" + safe(memory.getSourceTitle())
                + "\n\n用户备注：\n" + safe(memory.getNote())
                + "\n\n原始内容：\n" + safe(memory.getOriginalText());
        String systemPrompt = "你是个人知识管理系统的标签提取助手。\n"
                + "请从用户收藏的标题、备注和原始内容中提取用于检索的候选标签。\n"
                + "规则：只提取内容直接支持的标签；优先提取主题、对象、材料、技术、教程类型、用途和问题；"
                + "标签保持简短，一般2到12个汉字或常见英文词；返回3到8个；不要生成句子、解释或不存在的信息。\n"
                + "必须只返回严格 JSON：{\"tags\":[\"标签1\",\"标签2\"]}";
        try {
            String body = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0.1)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                            .add(objectMapper.createObjectNode().put("role", "user").put("content", userContent)))
                    .toString();
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            content = content.replace("```json", "").replace("```", "").trim();
            JsonNode tagsNode = objectMapper.readTree(content).path("tags");
            List<String> tags = new ArrayList<>();
            if (tagsNode.isArray()) for (JsonNode tag : tagsNode) {
                String value = tag.asText("").trim();
                if (!value.isBlank() && value.length() <= 100 && !tags.contains(value)) tags.add(value);
            }
            return tags.subList(0, Math.min(tags.size(), 8));
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 标签分析失败: " + e.getMessage(), e);
        }
    }

    private String safe(String value) { return value == null ? "" : value; }
}
