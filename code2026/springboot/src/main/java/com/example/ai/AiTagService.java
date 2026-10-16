package com.example.ai;

import java.util.List;

public interface AiTagService {
    List<String> suggestTags(Long memoryId);
}
