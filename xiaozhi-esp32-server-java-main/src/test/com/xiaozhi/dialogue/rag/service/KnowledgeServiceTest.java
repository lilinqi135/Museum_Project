package com.xiaozhi.dialogue.rag.service;

import com.xiaozhi.dao.KnowledgeChunkMapper;
import com.xiaozhi.dao.KnowledgeDocumentMapper;
import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.entity.SysKnowledgeDocument;
import com.xiaozhi.service.SysExhibitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceTest {

    @Mock
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Mock
    private KnowledgeChunkMapper knowledgeChunkMapper;
    @Mock
    private KnowledgeIndexingService knowledgeIndexingService;
    @Mock
    private RagProperties ragProperties;
    @Mock
    private SysExhibitService exhibitService;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        knowledgeService = new KnowledgeService(
                knowledgeDocumentMapper,
                knowledgeChunkMapper,
                knowledgeIndexingService,
                ragProperties,
                exhibitService
        );
    }

    @Test
    void uploadAndIndexShouldRejectInvalidExhibitOwnership() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bronze.txt",
                "text/plain",
                "青铜尊资料".getBytes()
        );
        when(exhibitService.exists(10L, 1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.uploadAndIndex(file, 1L, 10L)
        );

        assertEquals("展品不存在或不属于当前场馆", exception.getMessage());
        verify(knowledgeDocumentMapper, never()).insert(any());
    }

    @Test
    void bindDocumentToExhibitShouldRejectCrossMuseumDocument() {
        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(2L);
        when(exhibitService.exists(10L, 1L)).thenReturn(true);
        when(knowledgeDocumentMapper.selectById(100L)).thenReturn(document);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.bindDocumentToExhibit(100L, 1L, 10L)
        );

        assertEquals("文档不属于当前场馆", exception.getMessage());
        verify(knowledgeDocumentMapper, never()).updateExhibitId(any(), any(), any());
    }

    @Test
    void bindDocumentToExhibitShouldReindexWhenBindingSucceeds() {
        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(1L);
        when(exhibitService.exists(10L, 1L)).thenReturn(true);
        when(knowledgeDocumentMapper.selectById(100L)).thenReturn(document);
        when(knowledgeDocumentMapper.updateExhibitId(100L, 1L, 10L)).thenReturn(1);

        knowledgeService.bindDocumentToExhibit(100L, 1L, 10L);

        verify(knowledgeChunkMapper).deleteByDocumentId(100L);
        verify(knowledgeDocumentMapper).updateStatusAndChunk(100L, "PENDING", 0, null);
        verify(knowledgeIndexingService).indexDocumentAsync(100L);
    }

    @Test
    void uploadAndIndexShouldBindDuplicateDocumentWhenOriginalHasNoExhibit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bronze.txt",
                "text/plain",
                "青铜尊资料".getBytes()
        );
        SysKnowledgeDocument duplicated = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(1L)
                .setStatus("COMPLETED")
                .setExhibitId(null);
        SysKnowledgeDocument reboundDocument = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(1L)
                .setStatus("PENDING")
                .setExhibitId(10L);
        when(exhibitService.exists(10L, 1L)).thenReturn(true);
        when(knowledgeDocumentMapper.selectByMuseumAndHash(eq(1L), any())).thenReturn(duplicated);
        when(knowledgeDocumentMapper.selectById(100L)).thenReturn(reboundDocument);

        SysKnowledgeDocument result = knowledgeService.uploadAndIndex(file, 1L, 10L);

        assertEquals(10L, result.getExhibitId());
        verify(knowledgeDocumentMapper).updateExhibitId(100L, 1L, 10L);
        verify(knowledgeChunkMapper).deleteByDocumentId(100L);
        verify(knowledgeDocumentMapper).updateStatusAndChunk(100L, "PENDING", 0, null);
        verify(knowledgeIndexingService).indexDocumentAsync(100L);
        verify(knowledgeDocumentMapper, never()).insert(any());
    }

    @Test
    void uploadAndIndexShouldRejectDuplicateDocumentBoundToAnotherExhibit() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bronze.txt",
                "text/plain",
                "青铜尊资料".getBytes()
        );
        SysKnowledgeDocument duplicated = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(1L)
                .setStatus("COMPLETED")
                .setExhibitId(99L);
        when(exhibitService.exists(10L, 1L)).thenReturn(true);
        when(knowledgeDocumentMapper.selectByMuseumAndHash(eq(1L), any())).thenReturn(duplicated);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.uploadAndIndex(file, 1L, 10L)
        );

        assertEquals("相同文档已绑定到其他展品，请先解绑后再调整归属", exception.getMessage());
        verify(knowledgeDocumentMapper, never()).insert(any());
    }

    @Test
    void listByExhibitShouldRejectInvalidExhibitOwnership() {
        when(exhibitService.exists(10L, 1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.listByExhibit(1L, 10L, 1, 10)
        );

        assertEquals("展品不存在或不属于当前场馆", exception.getMessage());
        verify(knowledgeDocumentMapper, never()).selectByExhibitPaged(eq(1L), eq(10L), any(), any());
    }

    @Test
    void listShouldRejectUnsupportedStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.list(1L, "done", 1, 10)
        );

        assertEquals("status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED", exception.getMessage());
    }

    @Test
    void getStatusShouldRejectCrossMuseumDocument() {
        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(2L);
        when(knowledgeDocumentMapper.selectById(100L)).thenReturn(document);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.getStatus(100L, 1L)
        );

        assertEquals("文档不属于当前场馆", exception.getMessage());
    }

    @Test
    void deleteDocumentShouldRejectCrossMuseumDocument() {
        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(2L);
        when(knowledgeDocumentMapper.selectById(100L)).thenReturn(document);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> knowledgeService.deleteDocument(100L, 1L)
        );

        assertEquals("文档不属于当前场馆", exception.getMessage());
        verify(knowledgeChunkMapper, never()).deleteByDocumentId(100L);
        verify(knowledgeDocumentMapper, never()).deleteById(100L);
    }
}
