package com.xiaozhi.common.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RAG 演示配置
 * 提供一个基础的 EmbeddingModel 避免启动失败
 */
@Configuration
public class RagDemoConfig {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public float[] embed(Document document) {
                if (document == null) {
                    return new float[EMBEDDING_DIMENSION];
                }
                return embed(document.getText());
            }

            @Override
            public float[] embed(String text) {
                if (text == null || text.isBlank()) {
                    return new float[EMBEDDING_DIMENSION];
                }

                float[] vector = new float[EMBEDDING_DIMENSION];
                String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                char[] chars = normalized.toCharArray();

                for (int i = 0; i < chars.length; i++) {
                    accumulate(vector, String.valueOf(chars[i]), 1.0f);
                    if (i + 1 < chars.length) {
                        accumulate(vector, normalized.substring(i, i + 2), 1.5f);
                    }
                }

                normalize(vector);
                return vector;
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                return new EmbeddingResponse(new ArrayList<>());
            }
        };
    }

    private static void accumulate(float[] vector, String token, float weight) {
        int index = Math.floorMod(token.hashCode(), EMBEDDING_DIMENSION);
        vector[index] += weight;
    }

    private static void normalize(float[] vector) {
        double sum = 0D;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0D) {
            return;
        }

        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
