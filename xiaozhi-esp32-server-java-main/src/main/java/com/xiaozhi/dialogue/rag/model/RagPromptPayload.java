package com.xiaozhi.dialogue.rag.model;

import org.springframework.util.StringUtils;

/**
 * RAG Prompt 载荷
 */
public record RagPromptPayload(String promptText, String contextText) {

    public RagPromptPayload {
        promptText = promptText == null ? "" : promptText;
        contextText = contextText == null ? "" : contextText;
    }

    public boolean hasKnowledge() {
        return StringUtils.hasText(contextText);
    }
}
