package com.xiaozhi.dialogue.llm.tool.config;

import com.xiaozhi.dialogue.llm.tool.XiaoZhiToolCallingManager;
import com.xiaozhi.dialogue.llm.tool.observation.ChatModelObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallConfig {

    private final ToolExecutionExceptionProcessor defaultToolExecutionExceptionProcessor
            = DefaultToolExecutionExceptionProcessor.builder().build();

    @Bean
    public ToolCallingManager toolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
                                                 @Autowired(required = false) ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
        // Create and return a ToolCallingManager instance
        // This is a placeholder; actual implementation may vary based on requirements
        return new XiaoZhiToolCallingManager(observationRegistry, toolCallbackResolver,
                toolExecutionExceptionProcessor == null ? defaultToolExecutionExceptionProcessor : toolExecutionExceptionProcessor);
    }
    
    /**
     * 注册聊天模型观察处理器
     * 这个Bean会自动被Spring的ObservationRegistry发现并注册
     * 用于在流式响应完成后添加 AssistantMessage 到 Conversation
     */
    @Bean
    public ChatModelObservationHandler chatModelObservationHandler() {
        return new ChatModelObservationHandler();
    }

}
