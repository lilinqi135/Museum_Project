package com.xiaozhi.dialogue.llm.factory.providers;

import com.xiaozhi.dialogue.llm.factory.ChatModelProvider;
import com.xiaozhi.dialogue.llm.providers.XingChenChatModel;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 星辰(讯飞)模型提供者
 */
@Component
public class XingChenModelProvider implements ChatModelProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(XingChenModelProvider.class);
    
    @Autowired
    private SysConfigService configService;
    
    @Override
    public String getProviderName() {
        return "xingchen";
    }
    
    @Override
    public ChatModel createChatModel(SysConfig config, SysRole role) {
        String endpoint = config.getApiUrl();
        
        // XingChen需要查询agent配置获取ApiKey和Secret
        SysConfig agentConfig = new SysConfig()
                .setConfigType("agent")
                .setProvider("xingchen")
                .setUserId(config.getUserId());
        
        SysConfig queryConfig = configService.query(agentConfig, null).get(0);
        String apiKey = queryConfig.getApiKey();
        String apiSecret = queryConfig.getApiSecret();
        
        var chatModel = new XingChenChatModel(endpoint, apiKey, apiSecret);
        
        logger.info("Created XingChen ChatModel: endpoint={}", endpoint);
        return chatModel;
    }
}

