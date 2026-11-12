package com.pageturner.bookstore.ai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    EmbeddingModel embeddingModel(AiProperties p) {
        if ("local".equalsIgnoreCase(p.embeddings())) {
            return new HashingEmbeddingModel();
        }
        // Builder is lazy: the app starts fine without Ollama; only the recommendations endpoint needs it.
        return OllamaEmbeddingModel.builder()
                .baseUrl(p.baseUrl())
                .modelName(p.embeddingModel())
                .timeout(p.timeout())
                .build();
    }
}
