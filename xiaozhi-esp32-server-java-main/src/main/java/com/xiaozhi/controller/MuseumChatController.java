package com.xiaozhi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.common.web.ResultStatus;
import com.xiaozhi.dialogue.llm.factory.ChatModelFactory;
import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.dialogue.rag.model.RagPromptPayload;
import com.xiaozhi.dialogue.rag.service.MuseumRagPromptService;
import com.xiaozhi.dialogue.service.MuseumRagsService;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysExhibitService;
import com.xiaozhi.service.SysDeviceService;
import com.xiaozhi.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 博物馆导览对话测试控制器
 * 用于 Postman 测试 RAG 效果
 */
@RestController
@RequestMapping("/api/museum")
@Tag(name = "博物馆导览", description = "展品对话与知识库测试")
public class MuseumChatController {

    private static final Logger logger = LoggerFactory.getLogger(MuseumChatController.class);
    private static final Set<String> SUPPORTED_MODES = Set.of("visitor", "edu", "kids");

    @Resource
    private MuseumRagsService museumRagsService;

    @Resource
    private ChatModelFactory chatModelFactory;

    @Resource
    private MuseumRagPromptService ragPromptService;
    @Resource
    private RagProperties ragProperties;

    @Resource
    private SysRoleService roleService;
    @Resource
    private SysDeviceService deviceService;
    @Resource
    private SysExhibitService exhibitService;

    @Data
    public static class MuseumChatParam {
        private Integer roleId;
        private Long museumId;
        /**
         * 展品ID（可选）。指定后优先检索该展品知识。
         */
        private Long exhibitId;
        /**
         * 设备ID（可选）。若未传 roleId/mode，会自动基于设备信息兜底推断。
         */
        private String deviceId;
        /**
         * 输出模式: visitor(默认) / edu / kids
         */
        private String mode;
        private String question;
        /**
         * 是否返回调试信息
         */
        private Boolean debug;
    }

    @SaIgnore
    @PostMapping("/chat")
    @Operation(summary = "展品对话测试", description = "Postman 测试接口：带 RAG 增强的对话")
    public ResultMessage chat(@RequestBody MuseumChatParam param) {
        long start = System.currentTimeMillis();
        try {
            if (param == null) {
                return badRequest("请求参数不能为空");
            }
            
            // 自动解析归属馆：如果未传 museumId 但传了 deviceId，则从设备信息中获取
            if (param.getMuseumId() == null && StringUtils.hasText(param.getDeviceId())) {
                SysDevice device = deviceService.selectDeviceById(param.getDeviceId());
                if (device != null) {
                    param.setMuseumId(device.getMuseumId());
                    // 如果参数没传 exhibitId，则也尝试从设备当前关联中获取
                    if (param.getExhibitId() == null) {
                        param.setExhibitId(device.getExhibitId());
                    }
                }
            }

            if (param.getMuseumId() == null || param.getMuseumId() <= 0) {
                return badRequest("museumId 不能为空，且无法通过 deviceId 自动推断归属馆");
            }
            String normalizedQuestion = normalizeQuestion(param.getQuestion());
            if (!StringUtils.hasText(normalizedQuestion)) {
                return badRequest("question 不能为空");
            }
            String requestedMode = normalizeMode(param.getMode());
            if (param.getMode() != null && requestedMode == null) {
                return badRequest("mode 仅支持 visitor、edu、kids");
            }
            if (param.getExhibitId() != null && !exhibitService.exists(param.getExhibitId(), param.getMuseumId())) {
                return badRequest("展品不存在或不属于当前场馆");
            }

            Integer resolvedRoleId = resolveRoleId(param);
            if (resolvedRoleId == null) {
                return badRequest("roleId 不能为空，且无法通过 deviceId 自动推断");
            }
            String resolvedMode = resolveMode(param, resolvedRoleId);

            long roleStart = System.currentTimeMillis();
            // 1. 获取角色配置
            SysRole role = roleService.selectRoleById(resolvedRoleId);
            if (role == null) {
                return badRequest("角色不存在");
            }
            long roleCost = System.currentTimeMillis() - roleStart;

            long ragStart = System.currentTimeMillis();
            // 2. 本地检索知识片段并组装 Prompt
            RagPromptPayload ragPromptPayload = ragPromptService.buildPrompt(normalizedQuestion, param.getMuseumId(), param.getExhibitId(), resolvedMode);
            String systemInstructions = ragPromptService.buildSystemInstructionsFromContext(
                    "你是一个专业的博物馆导览员。请根据提供的展品背景知识回答用户问题。",
                    ragPromptPayload.contextText(),
                    resolvedMode
            );
            
            // 方案A：增强 AI 的语境感知能力，实现语气自适应
            systemInstructions += "\n\n[核心交互原则 - 语气自适应感知]";
            systemInstructions += "\n请敏锐识别用户的表达风格并实时调整你的回复语气：";
            systemInstructions += "\n1. 若提问口吻稚嫩（如“这个好酷呀”、“它会动吗”），请自动切换为【儿童模式】：亲切、多用拟人化、通俗易懂，像讲故事一样。";
            systemInstructions += "\n2. 若提问专业严谨（如“铸造工艺”、“历史分期”），请切换为【专家模式】：严谨、学术、提供深度考证，回答更具厚度。";
            systemInstructions += "\n3. 若提问简洁直接，请保持【专业导览员】的中立与高效。";
            
            if (resolvedMode != null && !resolvedMode.equals("visitor")) {
                systemInstructions += "\n\n[当前设备提示] 该设备当前被标记为 " + resolvedMode + " 倾向，请在自适应识别不明显时优先参考此倾向。";
            }
            
            systemInstructions += "\n\n回答风格要求：先结论，再依据，再延展。优先给出3条以内要点，字数控制在120-180字。";
            systemInstructions += "\n缺少资料时请温和说明边界，并给出可继续探索的方向，不要机械地直接拒答。";
            long ragCost = System.currentTimeMillis() - ragStart;

            SystemMessage systemMessage = new SystemMessage(systemInstructions);
            UserMessage userMessage = new UserMessage(ragPromptPayload.promptText());
            
            // 4. 调用 LLM
            long llmStart = System.currentTimeMillis();
            ChatModel chatModel = chatModelFactory.takeChatModel(role);
            String answer = chatModel.call(new Prompt(List.of(systemMessage, userMessage))).getResult().getOutput().getText();
            long llmCost = System.currentTimeMillis() - llmStart;

            long total = System.currentTimeMillis() - start;
            logger.info("museum chat耗时统计 role={}ms rag={}ms llm={}ms total={}ms, mode={}, deviceId={}, exhibitId={}, question={}",
                    roleCost, ragCost, llmCost, total, resolvedMode, param.getDeviceId(), param.getExhibitId(), normalizedQuestion);

            ResultMessage result = ResultMessage.success("操作成功", answer);
            if (Boolean.TRUE.equals(param.getDebug()) && ragProperties.isDebugEnabled()) {
                Map<String, Object> debugInfo = new HashMap<>();
                debugInfo.put("roleCost", roleCost);
                debugInfo.put("ragCost", ragCost);
                debugInfo.put("llmCost", llmCost);
                debugInfo.put("totalCost", total);
                debugInfo.put("contextText", ragPromptPayload.contextText());
                debugInfo.put("hintMode", resolvedMode);
                debugInfo.put("requestedMuseumId", param.getMuseumId());
                debugInfo.put("requestedExhibitId", param.getExhibitId());
                debugInfo.put("hasKnowledge", ragPromptPayload.hasKnowledge());
                debugInfo.put("contextSources", extractContextSources(ragPromptPayload.contextText()));
                debugInfo.put("adaptiveStrategy", "Option A: System Prompt based adaptive tone");
                result.put("debug", debugInfo);
            }
            return result;
        } catch (Exception e) {
            long total = System.currentTimeMillis() - start;
            logger.error("museum chat失败, total={}ms", total, e);
            return ResultMessage.error("对话失败: " + e.getMessage());
        }
    }

    private ResultMessage badRequest(String message) {
        return ResultMessage.error(ResultStatus.BAD_REQUEST, message);
    }

    @SaIgnore
    @PostMapping("/refresh")
    @Operation(summary = "刷新知识库", description = "将数据库中的展品信息重新加载到向量库")
    public ResultMessage refresh() {
        museumRagsService.refreshKnowledgeBase();
        return ResultMessage.success("知识库刷新成功");
    }

    private Integer resolveRoleId(MuseumChatParam param) {
        if (param.getRoleId() != null) {
            return param.getRoleId();
        }
        if (!StringUtils.hasText(param.getDeviceId())) {
            return null;
        }
        SysDevice device = deviceService.selectDeviceById(param.getDeviceId());
        return device == null ? null : device.getRoleId();
    }

    private String resolveMode(MuseumChatParam param, Integer roleId) {
        String requestedMode = normalizeMode(param.getMode());
        if (StringUtils.hasText(requestedMode)) {
            return requestedMode;
        }
        // 优先依据角色名称自动推断模式，避免游客额外声明身份
        try {
            SysRole role = roleService.selectRoleById(roleId);
            if (role != null && StringUtils.hasText(role.getRoleName())) {
                String roleName = role.getRoleName().toLowerCase();
                if (roleName.contains("儿童") || roleName.contains("小朋友") || roleName.contains("kids")) {
                    return "kids";
                }
                if (roleName.contains("研学") || roleName.contains("老师") || roleName.contains("edu")) {
                    return "edu";
                }
            }
        } catch (Exception e) {
            logger.warn("自动推断mode失败，回退visitor: {}", e.getMessage());
        }
        return "visitor";
    }

    private String normalizeQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }
        return question.trim();
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return null;
        }
        String normalizedMode = mode.trim().toLowerCase();
        if (!SUPPORTED_MODES.contains(normalizedMode)) {
            return null;
        }
        return normalizedMode;
    }

    private List<String> extractContextSources(String contextText) {
        if (!StringUtils.hasText(contextText)) {
            return List.of();
        }
        Set<String> sources = new LinkedHashSet<>();
        String[] segments = contextText.split("\\n---\\n");
        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || !segment.startsWith("[")) {
                continue;
            }
            int endIndex = segment.indexOf("]:");
            if (endIndex <= 1) {
                continue;
            }
            sources.add(segment.substring(1, endIndex));
        }
        return List.copyOf(sources);
    }
}
