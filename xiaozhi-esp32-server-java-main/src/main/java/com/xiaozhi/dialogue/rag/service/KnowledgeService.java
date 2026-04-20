package com.xiaozhi.dialogue.rag.service;

import com.xiaozhi.dao.KnowledgeChunkMapper;
import com.xiaozhi.dao.KnowledgeDocumentMapper;
import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.entity.SysKnowledgeDocument;
import com.xiaozhi.service.SysExhibitService;
import com.xiaozhi.utils.FileHashUtil;
import com.xiaozhi.utils.FileUploadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SUPPORTED_DOCUMENT_STATUSES = Set.of("PENDING", "PROCESSING", "COMPLETED", "FAILED");

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeIndexingService knowledgeIndexingService;
    private final RagProperties ragProperties;
    private final SysExhibitService exhibitService;

    public KnowledgeService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                            KnowledgeChunkMapper knowledgeChunkMapper,
                            KnowledgeIndexingService knowledgeIndexingService,
                            RagProperties ragProperties,
                            SysExhibitService exhibitService) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeIndexingService = knowledgeIndexingService;
        this.ragProperties = ragProperties;
        this.exhibitService = exhibitService;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysKnowledgeDocument uploadAndIndex(MultipartFile file, Long museumId, Long exhibitId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (museumId == null || museumId <= 0) {
            throw new IllegalArgumentException("museumId 非法");
        }
        validateExhibitBinding(museumId, exhibitId);

        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            throw new IllegalArgumentException("文件名非法");
        }

        String suffix = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!("txt".equals(suffix) || "pdf".equals(suffix) || "docx".equals(suffix) || "md".equals(suffix))) {
            throw new IllegalArgumentException("仅支持 txt/pdf/docx/md 文件");
        }

        String fileHash = FileHashUtil.calculateSha256(file);
        SysKnowledgeDocument duplicated = knowledgeDocumentMapper.selectByMuseumAndHash(museumId, fileHash);
        if (duplicated != null) {
            return handleDuplicatedDocument(duplicated, museumId, exhibitId);
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String relativePath = "museum_" + museumId + "/" + datePath;
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        String filePath = FileUploadUtils.uploadFile(ragProperties.getDocsPath(), relativePath, newFileName, file);

        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setMuseumId(museumId)
                .setExhibitId(exhibitId)
                .setFileName(originalName)
                .setFileUrl(filePath)
                .setFileHash(fileHash)
                .setFileSize(file.getSize())
                .setStatus(knowledgeIndexingService.pendingStatus())
                .setChunkCount(0)
                .setErrorMsg(null);

        knowledgeDocumentMapper.insert(document);
        triggerIndexAfterCommit(document.getId());
        return document;
    }

    public Map<String, Object> list(Long museumId, String status, Integer page, Integer pageSize) {
        if (museumId == null || museumId <= 0) {
            throw new IllegalArgumentException("museumId 非法");
        }
        String normalizedStatus = normalizeStatus(status);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        List<SysKnowledgeDocument> records = knowledgeDocumentMapper.selectByMuseumPaged(museumId, normalizedStatus, offset, safeSize);
        long total = knowledgeDocumentMapper.countByMuseum(museumId, normalizedStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("total", total);
        result.put("records", records);
        return result;
    }

    public SysKnowledgeDocument getStatus(Long documentId, Long museumId) {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("documentId 非法");
        }
        if (museumId == null || museumId <= 0) {
            throw new IllegalArgumentException("museumId 非法");
        }
        SysKnowledgeDocument document = requireDocument(documentId);
        validateDocumentOwnership(document, museumId);
        return document;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId, Long museumId) {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("documentId 非法");
        }
        if (museumId == null || museumId <= 0) {
            throw new IllegalArgumentException("museumId 非法");
        }
        SysKnowledgeDocument document = requireDocument(documentId);
        validateDocumentOwnership(document, museumId);

        List<String> vectorIds = knowledgeChunkMapper.selectVectorIdsByDocumentId(documentId);
        try {
            knowledgeIndexingService.deleteVectorsByIds(vectorIds);
        } catch (Exception e) {
            logger.warn("精确删除向量失败，将回退重建向量库, documentId={}, error={}", documentId, e.getMessage());
            knowledgeIndexingService.rebuildVectorStoreFromDb();
        }

        knowledgeChunkMapper.deleteByDocumentId(documentId);
        knowledgeDocumentMapper.deleteById(documentId);
        deleteLocalFileQuietly(document.getFileUrl());
    }

    public void rebuildByMuseum(Long museumId) {
        if (museumId == null || museumId <= 0) {
            throw new IllegalArgumentException("museumId 非法");
        }
        List<SysKnowledgeDocument> documents = knowledgeDocumentMapper.selectByMuseumId(museumId);
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (SysKnowledgeDocument document : documents) {
            knowledgeChunkMapper.deleteByDocumentId(document.getId());
            knowledgeDocumentMapper.updateStatusAndChunk(document.getId(), "PENDING", 0, null);
            triggerIndexAfterCommit(document.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindDocumentToExhibit(Long documentId, Long museumId, Long exhibitId) {
        if (documentId == null || museumId == null || exhibitId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (museumId <= 0 || exhibitId <= 0 || documentId <= 0) {
            throw new IllegalArgumentException("参数非法");
        }
        validateExhibitBinding(museumId, exhibitId);
        SysKnowledgeDocument document = requireDocument(documentId);
        if (!museumId.equals(document.getMuseumId())) {
            throw new IllegalArgumentException("文档不属于当前场馆");
        }
        int row = knowledgeDocumentMapper.updateExhibitId(documentId, museumId, exhibitId);
        if (row > 0) {
            // 绑定后需要重新索引，因为元数据里的 exhibitId 变了
            knowledgeChunkMapper.deleteByDocumentId(documentId);
            knowledgeDocumentMapper.updateStatusAndChunk(documentId, "PENDING", 0, null);
            triggerIndexAfterCommit(documentId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindDocumentExhibit(Long documentId, Long museumId) {
        if (documentId == null || museumId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (documentId <= 0 || museumId <= 0) {
            throw new IllegalArgumentException("参数非法");
        }
        SysKnowledgeDocument document = requireDocument(documentId);
        if (!museumId.equals(document.getMuseumId())) {
            throw new IllegalArgumentException("文档不属于当前场馆");
        }
        int row = knowledgeDocumentMapper.unbindExhibitId(documentId, museumId);
        if (row > 0) {
            // 解绑后同样需要重新索引以清除元数据里的 exhibitId
            knowledgeChunkMapper.deleteByDocumentId(documentId);
            knowledgeDocumentMapper.updateStatusAndChunk(documentId, "PENDING", 0, null);
            triggerIndexAfterCommit(documentId);
        }
    }

    public Map<String, Object> listByExhibit(Long museumId, Long exhibitId, Integer page, Integer pageSize) {
        if (museumId == null || exhibitId == null) {
            throw new IllegalArgumentException("museumId 或 exhibitId 不能为空");
        }
        validateExhibitBinding(museumId, exhibitId);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        List<SysKnowledgeDocument> records = knowledgeDocumentMapper.selectByExhibitPaged(museumId, exhibitId, offset, safeSize);
        long total = knowledgeDocumentMapper.countByExhibit(museumId, exhibitId);

        Map<String, Object> result = new HashMap<>();
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("total", total);
        result.put("records", records);
        return result;
    }

    private void validateExhibitBinding(Long museumId, Long exhibitId) {
        if (exhibitId == null) {
            return;
        }
        if (museumId == null || museumId <= 0 || exhibitId <= 0) {
            throw new IllegalArgumentException("展品绑定参数非法");
        }
        if (!exhibitService.exists(exhibitId, museumId)) {
            throw new IllegalArgumentException("展品不存在或不属于当前场馆");
        }
    }

    private SysKnowledgeDocument requireDocument(Long documentId) {
        SysKnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        return document;
    }

    private void validateDocumentOwnership(SysKnowledgeDocument document, Long museumId) {
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!museumId.equals(document.getMuseumId())) {
            throw new IllegalArgumentException("文档不属于当前场馆");
        }
    }

    private SysKnowledgeDocument handleDuplicatedDocument(SysKnowledgeDocument duplicated, Long museumId, Long exhibitId) {
        if (duplicated == null) {
            throw new IllegalArgumentException("文档不存在");
        }

        if ("FAILED".equalsIgnoreCase(duplicated.getStatus())) {
            // 已失败的重复文档允许重新走一次索引，避免用户只能手工清理数据库
            knowledgeChunkMapper.deleteByDocumentId(duplicated.getId());
            knowledgeDocumentMapper.updateStatusAndChunk(duplicated.getId(), knowledgeIndexingService.pendingStatus(), 0, null);
            triggerIndexAfterCommit(duplicated.getId());
            return requireDocument(duplicated.getId());
        }

        if (exhibitId == null || Objects.equals(duplicated.getExhibitId(), exhibitId)) {
            return duplicated;
        }

        if (duplicated.getExhibitId() == null) {
            validateExhibitBinding(museumId, exhibitId);
            knowledgeDocumentMapper.updateExhibitId(duplicated.getId(), museumId, exhibitId);
            knowledgeChunkMapper.deleteByDocumentId(duplicated.getId());
            knowledgeDocumentMapper.updateStatusAndChunk(duplicated.getId(), knowledgeIndexingService.pendingStatus(), 0, null);
            triggerIndexAfterCommit(duplicated.getId());
            return requireDocument(duplicated.getId());
        }

        throw new IllegalArgumentException("相同文档已绑定到其他展品，请先解绑后再调整归属");
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_DOCUMENT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED");
        }
        return normalizedStatus;
    }

    private void triggerIndexAfterCommit(Long documentId) {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("documentId 非法");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            knowledgeIndexingService.indexDocumentAsync(documentId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                knowledgeIndexingService.indexDocumentAsync(documentId);
            }
        });
    }

    private void deleteLocalFileQuietly(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        try {
            File file = new File(fileUrl);
            if (file.exists() && file.isFile() && !file.delete()) {
                logger.warn("删除本地文档失败, file={}", fileUrl);
            }
        } catch (Exception e) {
            logger.warn("删除本地文档异常, file={}, error={}", fileUrl, e.getMessage());
        }
    }
}
