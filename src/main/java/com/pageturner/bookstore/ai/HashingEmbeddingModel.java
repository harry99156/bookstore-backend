package com.pageturner.bookstore.ai;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic bag-of-words hashing embedder. Needs no external service, so tests and
 * Ollama-less deployments still get (keyword-level) recommendations. Not semantic.
 */
public class HashingEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 512;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        return Response.from(segments.stream().map(s -> hash(s.text())).toList());
    }

    private static Embedding hash(String text) {
        float[] v = new float[DIMENSIONS];
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() > 2) {
                v[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        return Embedding.from(v);
    }
}
