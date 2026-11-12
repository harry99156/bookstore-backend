package com.pageturner.bookstore.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param embeddings     "ollama" (real semantic embeddings) or "local" (offline keyword-hash fallback)
 * @param baseUrl        Ollama server URL
 * @param embeddingModel Ollama embedding model name
 * @param timeout        per-request timeout for Ollama
 */
@ConfigurationProperties("bookstore.ai")
public record AiProperties(
        @DefaultValue("ollama") String embeddings,
        @DefaultValue("http://localhost:11434") String baseUrl,
        @DefaultValue("nomic-embed-text") String embeddingModel,
        @DefaultValue("60s") Duration timeout) {
}
