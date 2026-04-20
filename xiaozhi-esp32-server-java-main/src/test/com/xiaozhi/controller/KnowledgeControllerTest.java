package com.xiaozhi.controller;

import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.dialogue.rag.service.KnowledgeService;
import com.xiaozhi.entity.SysKnowledgeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeControllerTest {

    @Mock
    private KnowledgeService knowledgeService;

    private KnowledgeController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new KnowledgeController();
        ReflectionTestUtils.setField(controller, "knowledgeService", knowledgeService);
    }

    @Test
    void statusShouldPassMuseumIdToService() {
        SysKnowledgeDocument document = new SysKnowledgeDocument()
                .setId(100L)
                .setMuseumId(1L)
                .setStatus("COMPLETED");
        when(knowledgeService.getStatus(100L, 1L)).thenReturn(document);

        ResultMessage result = controller.status(100L, 1L);

        assertEquals("操作成功", result.getMessage());
        verify(knowledgeService).getStatus(100L, 1L);
    }

    @Test
    void deleteShouldPassMuseumIdToService() {
        ResultMessage result = controller.delete(100L, 1L);

        assertEquals("删除成功", result.getMessage());
        verify(knowledgeService).deleteDocument(100L, 1L);
    }

    @Test
    void listShouldReturnErrorWhenStatusIsUnsupported() {
        when(knowledgeService.list(1L, "done", 1, 10))
                .thenThrow(new IllegalArgumentException("status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED"));

        ResultMessage result = controller.list(1L, "done", 1, 10);

        assertEquals(400, result.getCode());
        assertEquals("status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED", result.getMessage());
    }

    @Test
    void uploadShouldReturnServiceErrorMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate.md",
                "text/markdown",
                "重复资料".getBytes()
        );
        when(knowledgeService.uploadAndIndex(file, 1L, 10L))
                .thenThrow(new IllegalArgumentException("相同文档已绑定到其他展品，请先解绑后再调整归属"));

        ResultMessage result = controller.upload(file, 1L, 10L);

        assertEquals(400, result.getCode());
        assertEquals("相同文档已绑定到其他展品，请先解绑后再调整归属", result.getMessage());
    }
}
