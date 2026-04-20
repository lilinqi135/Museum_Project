package com.xiaozhi.dialogue.rag.service;

import com.xiaozhi.dao.KnowledgeChunkMapper;
import com.xiaozhi.dao.KnowledgeDocumentMapper;
import com.xiaozhi.dialogue.llm.factory.EmbeddingModelFactory;
import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.entity.SysKnowledgeChunk;
import com.xiaozhi.entity.SysKnowledgeDocument;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
public class KnowledgeIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeIndexingService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;
    @Resource
    private EmbeddingModelFactory embeddingModelFactory;
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private RagProperties ragProperties;

    private final Object vectorLock = new Object();
    private volatile VectorStore vectorStore;

    @PostConstruct
    public void init() {
        try {
            rebuildVectorStoreFromDb();
        } catch (Exception e) {
            // 数据库尚未初始化时允许应用先启动，后续建表后可手动触发重建
            synchronized (vectorLock) {
                this.vectorStore = SimpleVectorStore.builder(resolveEmbeddingModel()).build();
            }
            logger.warn("RAG 初始化跳过，可能尚未执行建表脚本: {}", e.getMessage());
        }
    }

    public VectorStore currentVectorStore() {
        return vectorStore;
    }

    @Async("applicationTaskExecutor")
    public void indexDocumentAsync(Long documentId) {
        if (documentId == null) {
            return;
        }
        processDocument(documentId);
    }

    public void rebuildVectorStoreFromDb() {
        EmbeddingModel activeEmbeddingModel = resolveEmbeddingModel();
        VectorStore newStore = SimpleVectorStore.builder(activeEmbeddingModel).build();
        List<SysKnowledgeChunk> chunks;
        try {
            chunks = knowledgeChunkMapper.selectAllCompletedChunks();
        } catch (Exception e) {
            synchronized (vectorLock) {
                this.vectorStore = newStore;
            }
            logger.warn("读取 RAG 切片失败，向量库保持空状态: {}", e.getMessage());
            return;
        }
        if (chunks != null && !chunks.isEmpty()) {
            List<Document> documents = chunks.stream()
                    .filter(chunk -> StringUtils.hasText(chunk.getContent()))
                    .map(chunk -> this.toVectorDocument(chunk))
                    .collect(Collectors.toList());
            if (!documents.isEmpty()) {
                newStore.add(documents);
            }
        }
        synchronized (vectorLock) {
            this.vectorStore = newStore;
        }
        logger.info("RAG 向量库重建完成，切片数: {}", chunks == null ? 0 : chunks.size());
    }

    public List<Document> retrieveDocuments(String query, int topK, Long museumId) {
        return retrieveDocuments(query, topK, museumId, null);
    }

    public List<Document> retrieveDocuments(String query, int topK, Long museumId, Long exhibitId) {
        long start = System.currentTimeMillis();
        if (!ragProperties.isEnabled() || !StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        VectorStore store = vectorStore;
        if (store == null) {
            return Collections.emptyList();
        }

        List<Document> searchResult = store.similaritySearch(query);
        if (searchResult == null || searchResult.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 过滤博物馆
        List<Document> museumMatched = searchResult.stream()
                .filter(document -> filterByMuseum(document, museumId))
                .collect(Collectors.toList());

        // 2. 尝试过滤展品
        List<Document> exhibitMatched = museumMatched.stream()
                .filter(document -> filterByExhibit(document, exhibitId))
                .collect(Collectors.toList());

        // 3. 策略：优先展品命中，展品无命中时退回到博物馆级命中
        List<Document> finalResult;
        if (exhibitId != null && !exhibitMatched.isEmpty()) {
            finalResult = exhibitMatched;
        } else {
            finalResult = museumMatched;
        }

        finalResult = finalResult.stream()
                .limit(Math.max(1, topK))
                .collect(Collectors.toList());

        logger.info("向量检索耗时={}ms, 原始命中={}, 博物馆命中={}, 展品命中={}, topK={}, exhibitId={}",
                System.currentTimeMillis() - start, searchResult.size(), museumMatched.size(), exhibitMatched.size(), topK, exhibitId);
        return finalResult;
    }

    public void deleteVectorsByIds(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        VectorStore store = vectorStore;
        if (store == null) {
            return;
        }
        List<String> validIds = vectorIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return;
        }
        synchronized (vectorLock) {
            store.delete(validIds);
        }
    }

    private boolean filterByMuseum(Document document, Long museumId) {
        if (museumId == null) {
            return true;
        }
        Object metadataMuseumId = document.getMetadata().get("museumId");
        if (metadataMuseumId == null) {
            return false;
        }
        return Objects.equals(String.valueOf(museumId), String.valueOf(metadataMuseumId));
    }

    private boolean filterByExhibit(Document document, Long exhibitId) {
        if (exhibitId == null) {
            return true;
        }
        Object metadataExhibitId = document.getMetadata().get("exhibitId");
        if (metadataExhibitId == null) {
            return false;
        }
        return Objects.equals(String.valueOf(exhibitId), String.valueOf(metadataExhibitId));
    }

    private void processDocument(Long documentId) {
        SysKnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            logger.warn("索引文档不存在, documentId={}", documentId);
            return;
        }

        knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_PROCESSING, 0, null);
        knowledgeChunkMapper.deleteByDocumentId(documentId);

        File file = new File(document.getFileUrl());
        if (!file.exists() || !file.isFile()) {
            knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_FAILED, 0, "文件不存在或不可读取");
            logger.error("索引失败，文件不存在, documentId={}, file={}", documentId, document.getFileUrl());
            return;
        }

        try {
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(file));
            List<Document> rawDocs = reader.get();
            if (rawDocs == null || rawDocs.isEmpty()) {
                knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_FAILED, 0, "文档解析后无有效内容");
                return;
            }

            TokenTextSplitter splitter = new TokenTextSplitter(
                    ragProperties.getIndexing().getChunkSize(),
                    ragProperties.getIndexing().getChunkOverlap(),
                    5,
                    10000,
                    true
            );

            List<Document> splitDocs = splitter.apply(rawDocs);
            if (splitDocs == null || splitDocs.isEmpty()) {
                knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_FAILED, 0, "分块后无有效内容");
                return;
            }

            List<Document> finalDocs = new ArrayList<>();
            int chunkIndex = 0;
            int minChunkLength = Math.max(1, ragProperties.getIndexing().getMinChunkLength());
            for (Document splitDoc : splitDocs) {
                String text = splitDoc.getText();
                if (!StringUtils.hasText(text) || text.length() < minChunkLength) {
                    continue;
                }

                Map<String, Object> metadata = new HashMap<>(splitDoc.getMetadata());
                metadata.put("documentId", documentId);
                metadata.put("museumId", document.getMuseumId());
                metadata.put("exhibitId", document.getExhibitId());
                metadata.put("source", document.getFileName());
                metadata.put("chunkIndex", chunkIndex);

                String vectorId = UUID.randomUUID().toString();
                Document vectorDoc = Document.builder()
                        .id(vectorId)
                        .text(text)
                        .metadata(metadata)
                        .build();
                finalDocs.add(vectorDoc);

                SysKnowledgeChunk chunk = new SysKnowledgeChunk()
                        .setDocumentId(documentId)
                        .setChunkIndex(chunkIndex)
                        .setContent(text)
                        .setVectorId(vectorId);
                knowledgeChunkMapper.insert(chunk);
                chunkIndex++;
            }

            if (finalDocs.isEmpty()) {
                knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_FAILED, 0, "有效切片为空");
                return;
            }

            synchronized (vectorLock) {
                if (vectorStore == null) {
                    vectorStore = SimpleVectorStore.builder(resolveEmbeddingModel()).build();
                }
                vectorStore.add(finalDocs);
            }

            knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_COMPLETED, finalDocs.size(), null);
            logger.info("文档索引完成, documentId={}, chunkCount={}", documentId, finalDocs.size());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (!StringUtils.hasText(errorMsg)) {
                errorMsg = e.getClass().getSimpleName();
            }
            knowledgeDocumentMapper.updateStatusAndChunk(documentId, STATUS_FAILED, 0, errorMsg);
            logger.error("文档索引失败, documentId={}", documentId, e);
        }
    }

    private Document toVectorDocument(SysKnowledgeChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        if (chunk.getMuseumId() != null) {
            metadata.put("museumId", chunk.getMuseumId());
        }
        if (chunk.getExhibitId() != null) {
            metadata.put("exhibitId", chunk.getExhibitId());
        }
        if (StringUtils.hasText(chunk.getSource())) {
            metadata.put("source", chunk.getSource());
        }
        String vectorId = StringUtils.hasText(chunk.getVectorId()) ? chunk.getVectorId() : UUID.randomUUID().toString();
        return Document.builder()
                .id(vectorId)
                .text(chunk.getContent())
                .metadata(metadata)
                .build();
    }

    private EmbeddingModel resolveEmbeddingModel() {
        try {
            EmbeddingModel model = embeddingModelFactory.defaultEmbeddingModel();
            logger.info("RAG 使用系统默认 embedding 配置");
            return model;
        } catch (Exception e) {
            logger.warn("未找到可用的 embedding 配置，回退到内置 embedding: {}", e.getMessage());
            return embeddingModel;
        }
    }

    public String pendingStatus() {
        return STATUS_PENDING;
    }
}
