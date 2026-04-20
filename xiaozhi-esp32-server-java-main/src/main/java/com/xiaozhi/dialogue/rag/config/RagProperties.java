package com.xiaozhi.dialogue.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xiaozhi.rag")
public class RagProperties {

    private boolean enabled = true;
    private boolean debugEnabled = true;
    private String docsPath = "docs/museum_knowledge";
    private Retrieval retrieval = new Retrieval();
    private Indexing indexing = new Indexing();

    @Data
    public static class Retrieval {
        private int topK = 2;
        private int maxContextChars = 2000;
    }

    @Data
    public static class Indexing {
        private int chunkSize = 800;
        private int chunkOverlap = 100;
        private int minChunkLength = 20;
    }
}
