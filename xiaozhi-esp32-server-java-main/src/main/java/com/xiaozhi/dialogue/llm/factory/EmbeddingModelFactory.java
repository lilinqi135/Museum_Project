package com.xiaozhi.dialogue.llm.factory;

import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysConfigService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class EmbeddingModelFactory {

    private final Logger logger = LoggerFactory.getLogger(EmbeddingModelFactory.class);
    @Autowired
    private SysConfigService configService;

    public EmbeddingModel takeEmbeddingModel(Integer configId) {
        //SysConfig config = configService.selectModelType("embedding");
        SysConfig config = configService.selectConfigById(configId);
        return takeEmbeddingModel(config);
    }

    public EmbeddingModel defaultEmbeddingModel() {
        SysConfig config = configService.selectModelType(SysConfig.ModelType.embedding.getValue());
        return takeEmbeddingModel(config);
    }

    public @NotNull EmbeddingModel takeEmbeddingModel(SysConfig config) {
        Assert.notNull(config, "未配置向量模型");
        String provider = normalizeProviderName(config.getProvider());
        switch (provider) {
            case "openai":
                return newOpenAiEmbeddingModel(config);
            case "ollama":
                return newOllamaEmbeddingModel(config);
            case "zhipu":
                return newZhipuEmbeddingModel(config);
            // 默认为 openai 协议
            default:
                return newOpenAiEmbeddingModel(config);
        }
    }

    private String normalizeProviderName(String providerName) {
        Assert.hasText(providerName, "向量模型Provider不能为空");
        return switch (providerName.toLowerCase()) {
            case "zhipuai" -> "zhipu";
            default -> providerName.toLowerCase();
        };
    }

    private EmbeddingModel newOllamaEmbeddingModel(SysConfig config) {
        String endpoint = config.getApiUrl();
        String model = config.getConfigName();
        var ollamaApi = OllamaApi.builder().baseUrl(endpoint).build();

        var ollamaOptions = OllamaEmbeddingOptions.builder()
                .model(model)
                .build();

        var embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .build();
        logger.info("Using Ollama EmbeddingModel baseUrl: {}, name:{}",endpoint, model);
        return embeddingModel;
    }

    private EmbeddingModel newOpenAiEmbeddingModel(SysConfig config) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");

        // LM Studio不支持Http/2，所以需要强制使用HTTP/1.1
        var openAiApi = OpenAiApi.builder()
                .apiKey(StringUtils.hasText(config.getApiKey()) ? new SimpleApiKey(config.getApiKey()) : new NoopApiKey())
                .baseUrl(config.getApiUrl())
                .headers(headers)
                .webClientBuilder(WebClient.builder()
                        // Force HTTP/1.1 for streaming
                        .clientConnector(new JdkClientHttpConnector(HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(30))
                                .build())))
                .restClientBuilder(RestClient.builder()
                        // Force HTTP/1.1 for non-streaming
                        .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(30))
                                .build())))
                .build();
        var openAiEmbeddingOptions = OpenAiEmbeddingOptions.builder().model(config.getConfigName()).build();

        var embeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED,openAiEmbeddingOptions);
        logger.info("Using OpenAi EmbeddingModel baseUrl: {}, name:{}",config.getApiUrl(), config.getConfigName());
        return embeddingModel;
    }
    private EmbeddingModel newZhipuEmbeddingModel(SysConfig config) {
        String endpoint = normalizeZhiPuBaseUrl(config.getApiUrl());
        String model = normalizeZhiPuModel(config.getConfigName());
        var zhiPuAiApi = ZhiPuAiApi.builder()
                .baseUrl(endpoint)
                .apiKey(config.getApiKey())
                .build();

        var zhipuAiEmbeddingOptions = ZhiPuAiEmbeddingOptions.builder().model(model).build();

        var chatModel = new ZhiPuAiEmbeddingModel(zhiPuAiApi, MetadataMode.EMBED,zhipuAiEmbeddingOptions,
                RetryUtils.DEFAULT_RETRY_TEMPLATE);
        logger.info("Using ZhiPuAi EmbeddingModel baseUrl: {}, name:{}, rawName:{}", endpoint, model, config.getConfigName());
        return chatModel;
    }

    private String normalizeZhiPuBaseUrl(String apiUrl) {
        if (!StringUtils.hasText(apiUrl)) {
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
        Assert.hasText(modelName, "智谱向量模型不能为空");

        String normalized = modelName.trim().toLowerCase();
        normalized = normalized.replace('_', '-');
        normalized = normalized.replaceAll("\\s+", "");

        return switch (normalized) {
            case "glm4.7", "glm-4.7", "glm47" -> "glm-4.7";
            case "glm4.7-flashx", "glm-4.7-flashx" -> "glm-4.7-flashx";
            default -> normalized;
        };
    }

}
