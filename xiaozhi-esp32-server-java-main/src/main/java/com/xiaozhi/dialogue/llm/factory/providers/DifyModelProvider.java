package com.xiaozhi.dialogue.llm.factory.providers;

import com.xiaozhi.dialogue.llm.factory.ChatModelProvider;
import com.xiaozhi.dialogue.llm.providers.DifyChatModel;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dify模型提供者
 */
@Component
public class DifyModelProvider implements ChatModelProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(DifyModelProvider.class);
    
    @Autowired
    private SysConfigService configService;
    
    @Override
    public String getProviderName() {
        return "dify";
    }
    
    @Override
    public ChatModel createChatModel(SysConfig config, SysRole role) {
        String endpoint = config.getApiUrl();
        
        // Dify需要查询agent配置获取ApiKey
        SysConfig agentConfig = new SysConfig()
                .setConfigType("agent")
                .setProvider("dify")
                .setUserId(config.getUserId());
        
        SysConfig queryConfig = configService.query(agentConfig, null).get(0);
        String apiKey = queryConfig.getApiKey();
        
        var chatModel = new DifyChatModel(endpoint, apiKey);
        
        logger.info("Created Dify ChatModel: endpoint={}", endpoint);
        return chatModel;
    }
}

