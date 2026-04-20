package com.xiaozhi.dialogue.rag.service;

import com.xiaozhi.dialogue.rag.model.RagPromptPayload;
import com.xiaozhi.dialogue.rag.prompt.MuseumRagPromptTemplate;
import com.xiaozhi.dialogue.service.MuseumRagsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 博物馆 RAG Prompt 组装服务
 */
@Service
public class MuseumRagPromptService {

    private static final Logger logger = LoggerFactory.getLogger(MuseumRagPromptService.class);

    private final MuseumRagsService museumRagsService;
    private final MuseumRagPromptTemplate promptTemplate;

    public MuseumRagPromptService(MuseumRagsService museumRagsService, MuseumRagPromptTemplate promptTemplate) {
        this.museumRagsService = museumRagsService;
        this.promptTemplate = promptTemplate;
    }

    public RagPromptPayload buildPrompt(String question) {
        return buildPrompt(question, null, null);
    }

    public RagPromptPayload buildPrompt(String question, Long museumId) {
        return buildPrompt(question, museumId, null);
    }

    public RagPromptPayload buildPrompt(String question, Long museumId, String mode) {
        return buildPrompt(question, museumId, null, mode);
    }

    public RagPromptPayload buildPrompt(String question, Long museumId, Long exhibitId, String mode) {
        if (!StringUtils.hasText(question)) {
            return new RagPromptPayload("", "");
        }

        String contextText = museumRagsService.retrieveContext(question, museumId, exhibitId);
        String promptText = promptTemplate.render(question, contextText, mode);
        if (StringUtils.hasText(contextText)) {
            logger.info("博物馆本地 RAG 检索成功, exhibitId={}", exhibitId);
        }
        return new RagPromptPayload(promptText, contextText);
    }

    public String buildSystemInstructions(String baseInstructions, String question) {
        if (!StringUtils.hasText(question)) {
            return baseInstructions;
        }
        String contextText = museumRagsService.retrieveContext(question);
        return buildSystemInstructionsFromContext(baseInstructions, contextText);
    }

    public String buildSystemInstructionsFromContext(String baseInstructions, String contextText) {
        return buildSystemInstructionsFromContext(baseInstructions, contextText, null);
    }

    public String buildSystemInstructionsFromContext(String baseInstructions, String contextText, String mode) {
        return promptTemplate.renderSystemInstructions(baseInstructions, contextText, mode);
    }
}
