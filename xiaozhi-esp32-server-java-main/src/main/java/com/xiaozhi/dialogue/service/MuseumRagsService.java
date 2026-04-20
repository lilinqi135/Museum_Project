package com.xiaozhi.dialogue.service;

import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.dialogue.rag.service.KnowledgeIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MuseumRagsService {

    private static final Logger logger = LoggerFactory.getLogger(MuseumRagsService.class);

    private final KnowledgeIndexingService knowledgeIndexingService;
    private final RagProperties ragProperties;

    public MuseumRagsService(KnowledgeIndexingService knowledgeIndexingService, RagProperties ragProperties) {
        this.knowledgeIndexingService = knowledgeIndexingService;
        this.ragProperties = ragProperties;
    }

    /**
     * 重新从数据库切片重建向量库。
     */
    public synchronized void refreshKnowledgeBase() {
        try {
            knowledgeIndexingService.rebuildVectorStoreFromDb();
        } catch (Exception e) {
            logger.error("刷新博物馆 RAG 向量库失败", e);
        }
    }

    public String retrieveContext(String query) {
        return retrieveContext(query, null);
    }

    public String retrieveContext(String query, Long museumId) {
        return retrieveContext(query, museumId, null);
    }

    public String retrieveContext(String query, Long museumId, Long exhibitId) {
        long start = System.currentTimeMillis();
        try {
            if (!ragProperties.isEnabled() || !StringUtils.hasText(query)) {
                return "";
            }

            int topK = Math.max(1, ragProperties.getRetrieval().getTopK());
            int maxContextChars = Math.max(200, ragProperties.getRetrieval().getMaxContextChars());
            List<Document> results = knowledgeIndexingService.retrieveDocuments(query, topK, museumId, exhibitId);
            if (results == null || results.isEmpty()) {
                logger.info("RAG检索完成，无命中 total={}ms", System.currentTimeMillis() - start);
                return "";
            }

            String context = results.stream()
                    .map(this::toContextLine)
                    .collect(Collectors.joining("\n---\n"));
            if (context.length() > maxContextChars) {
                String truncated = context.substring(0, maxContextChars);
                logger.info("RAG检索完成，命中={}，上下文截断后长度={}，total={}ms",
                        results.size(), truncated.length(), System.currentTimeMillis() - start);
                return truncated;
            }
            logger.info("RAG检索完成，命中={}，上下文长度={}，total={}ms",
                    results.size(), context.length(), System.currentTimeMillis() - start);
            return context;
        } catch (Exception e) {
            logger.error("检索展品知识库失败", e);
            return "";
        }
    }

    private String toContextLine(Document document) {
        Object source = document.getMetadata().getOrDefault("source", "未知来源");
        return "[" + source + "]: " + document.getText();
    }
}
