package com.pageturner.bookstore.ai;

import com.pageturner.bookstore.exception.AiUnavailableException;
import com.pageturner.bookstore.exception.InvalidRequestException;
import com.pageturner.bookstore.exception.ResourceNotFoundException;
import com.pageturner.bookstore.model.Book;
import com.pageturner.bookstore.repository.BookRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Semantic book recommendations: books are embedded once and ranked by cosine similarity to a
 * free-text query ("a gripping space epic") or to another book ("more like Dune").
 *
 * <p>The embedding cache is keyed by a hash of each book's text, so edits are re-embedded and deleted
 * books are pruned on the next request - no coupling to the catalog write path.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    public record Scored(Book book, double score) {
    }

    private record CachedEmbedding(int textHash, Embedding embedding) {
    }

    private final BookRepository books;
    private final EmbeddingModel embeddingModel;
    private final Map<Long, CachedEmbedding> cache = new ConcurrentHashMap<>();

    public RecommendationService(BookRepository books, EmbeddingModel embeddingModel) {
        this.books = books;
        this.embeddingModel = embeddingModel;
    }

    /** Recommend in-stock books for a natural-language description. */
    public List<Scored> byQuery(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("Provide 'query' or 'similarTo'");
        }
        List<Book> catalog = books.findAll();
        Map<Long, Embedding> index = index(catalog);
        Embedding q = embed(List.of(TextSegment.from(query))).get(0);
        return rank(catalog, index, q, null, limit);
    }

    /** Recommend in-stock books similar to an existing one (the book itself is excluded). */
    public List<Scored> similarTo(Long bookId, int limit) {
        List<Book> catalog = books.findAll();
        if (catalog.stream().noneMatch(b -> b.getId().equals(bookId))) {
            throw new ResourceNotFoundException("Book " + bookId + " not found");
        }
        Map<Long, Embedding> index = index(catalog);
        return rank(catalog, index, index.get(bookId), bookId, limit);
    }

    private List<Scored> rank(List<Book> catalog, Map<Long, Embedding> index, Embedding target,
                              Long excludeId, int limit) {
        List<Scored> scored = new ArrayList<>();
        for (Book b : catalog) {
            if (b.getStock() > 0 && !b.getId().equals(excludeId)) {
                scored.add(new Scored(b, cosine(target, index.get(b.getId()))));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    /** Ensures every current book has an up-to-date embedding; returns id -> embedding for the catalog. */
    private Map<Long, Embedding> index(List<Book> catalog) {
        cache.keySet().retainAll(catalog.stream().map(Book::getId).toList());

        List<Book> stale = catalog.stream().filter(b -> {
            CachedEmbedding c = cache.get(b.getId());
            return c == null || c.textHash() != text(b).hashCode();
        }).toList();

        if (!stale.isEmpty()) {
            log.info("Embedding {} new/changed book(s)", stale.size());
            List<Embedding> fresh = embed(stale.stream().map(b -> TextSegment.from(text(b))).toList());
            for (int i = 0; i < stale.size(); i++) {
                Book b = stale.get(i);
                cache.put(b.getId(), new CachedEmbedding(text(b).hashCode(), fresh.get(i)));
            }
        }

        Map<Long, Embedding> result = new ConcurrentHashMap<>();
        catalog.forEach(b -> result.put(b.getId(), cache.get(b.getId()).embedding()));
        return result;
    }

    private List<Embedding> embed(List<TextSegment> segments) {
        try {
            return embeddingModel.embedAll(segments).content();
        } catch (RuntimeException e) {
            log.warn("Embedding backend failed: {}", e.toString());
            throw new AiUnavailableException("AI recommendation service is unavailable, please try again later", e);
        }
    }

    static String text(Book b) {
        return b.getTitle() + " by " + b.getAuthor() + ". Genre: " + b.getGenre() + ". "
                + (b.getDescription() == null ? "" : b.getDescription());
    }

    static double cosine(Embedding a, Embedding b) {
        float[] x = a.vector();
        float[] y = b.vector();
        double dot = 0, nx = 0, ny = 0;
        for (int i = 0; i < x.length; i++) {
            dot += x[i] * y[i];
            nx += x[i] * x[i];
            ny += y[i] * y[i];
        }
        return nx == 0 || ny == 0 ? 0 : dot / (Math.sqrt(nx) * Math.sqrt(ny));
    }
}
