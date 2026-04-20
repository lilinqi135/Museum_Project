package com.xiaozhi.dialogue.llm.factory.providers;

import com.xiaozhi.dialogue.llm.factory.ChatModelProvider;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 智谱AI模型提供者
 */
@Component
public class ZhiPuModelProvider implements ChatModelProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZhiPuModelProvider.class);

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Override
    public String getProviderName() {
        return "zhipu";
    }

    @Override
    public ChatModel createChatModel(SysConfig config, SysRole role) {
        String endpoint = normalizeZhiPuBaseUrl(config.getApiUrl());
        String apiKey = config.getApiKey();
        String model = normalizeZhiPuModel(config.getConfigName());
        Double temperature = role.getTemperature();
        Double topP = role.getTopP();

        var zhiPuAiApi = ZhiPuAiApi.builder().baseUrl(endpoint).apiKey(apiKey).build();

        var zhipuAiChatOptions = ZhiPuAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .topP(topP)
                .build();

        var chatModel = new ZhiPuAiChatModel(zhiPuAiApi, zhipuAiChatOptions, toolCallingManager, RetryUtils.DEFAULT_RETRY_TEMPLATE, observationRegistry);

        logger.info("Created ZhiPu ChatModel: model={}, rawModel={}, endpoint={}", model, config.getConfigName(), endpoint);
        return chatModel;
    }

    private String normalizeZhiPuBaseUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return apiUrl;
        }

        String normalized = apiUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        }
        if (normalized.endsWith("/v4")) {
            normalized = normalized.substring(0, normalized.length() - "/v4".length());
        }
        return normalized;
    }

    private String normalizeZhiPuModel(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            throw new IllegalArgumentException("智谱聊天模型不能为空");
        }

        String normalized = modelName.trim().toLowerCase();
        normalized = normalized.replace('_', '-');
        normalized = normalized.replaceAll("\\s+", "");

        return switch (normalized) {
            case "glm4.7", "glm-4.7", "glm47" -> "glm-4.7";
            case "glm4.7-preview", "glm-4.7-preview" -> "glm-4.7-preview";
            case "glm4.7-coding-preview", "glm-4.7-coding-preview" -> "glm-4.7-coding-preview";
            case "glm5.1", "glm-5.1", "glm51" -> "glm-5.1";
            case "glm5-turbo", "glm-5-turbo", "glm5turbo" -> "glm-5-turbo";
            default -> normalized;
        };
    }

}
