package com.xiaozhi.dialogue.service;

import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.dialogue.rag.service.KnowledgeIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class MuseumRagsServiceTest {

    @Mock
    private KnowledgeIndexingService knowledgeIndexingService;
    @Mock
    private RagProperties ragProperties;
    @Mock
    private RagProperties.Retrieval retrieval;

    @InjectMocks
    private MuseumRagsService museumRagsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRefreshKnowledgeBase() {
        museumRagsService.refreshKnowledgeBase();
        verify(knowledgeIndexingService, atLeastOnce()).rebuildVectorStoreFromDb();
    }

    @Test
    void testRetrieveContext() {
        when(ragProperties.isEnabled()).thenReturn(true);
        when(ragProperties.getRetrieval()).thenReturn(retrieval);
        when(retrieval.getTopK()).thenReturn(4);
        when(retrieval.getMaxContextChars()).thenReturn(4000);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test.txt");
        when(knowledgeIndexingService.retrieveDocuments(anyString(), anyInt(), isNull()))
                .thenReturn(List.of(new Document("测试切片内容", metadata)));

        String context = museumRagsService.retrieveContext("关于青铜器的问题");
        assertNotNull(context);
    }
}
