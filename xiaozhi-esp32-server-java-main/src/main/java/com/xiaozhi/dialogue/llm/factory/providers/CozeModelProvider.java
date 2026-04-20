package com.xiaozhi.dialogue.llm.factory.providers;

import com.xiaozhi.dialogue.llm.factory.ChatModelProvider;
import com.xiaozhi.dialogue.llm.providers.CozeChatModel;
import com.xiaozhi.dialogue.token.factory.TokenServiceFactory;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Coze模型提供者
 */
@Component
public class CozeModelProvider implements ChatModelProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CozeModelProvider.class);
    
    @Autowired
    private SysConfigService configService;
    
    @Autowired
    private TokenServiceFactory tokenServiceFactory;
    
    @Override
    public String getProviderName() {
        return "coze";
    }
    
    @Override
    public ChatModel createChatModel(SysConfig config, SysRole role) {
        String model = config.getConfigName();
        
        // Coze需要查询agent配置获取Token
        SysConfig agentConfig = new SysConfig()
                .setConfigType("agent")
                .setProvider("coze")
                .setUserId(config.getUserId());
        
        SysConfig queryConfig = configService.query(agentConfig, null).get(0);
        String token = tokenServiceFactory.getTokenService(queryConfig).getToken();
        
        var chatModel = new CozeChatModel(token, model);
        
        logger.info("Created Coze ChatModel: model={}", model);
        return chatModel;
    }
}

