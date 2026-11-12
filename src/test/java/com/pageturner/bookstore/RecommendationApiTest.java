package com.pageturner.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the offline hashing embedder (profile "test"), so no Ollama is required. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private long createBook(String isbn, String title, String genre, String description, int stock) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("isbn", isbn, "title", title, "author", "A. Writer",
                "genre", genre, "description", description, "price", 10, "stock", stock));
        String res = mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test
    void queryRanksMostRelevantBookFirstAndSkipsOutOfStock() throws Exception {
        createBook("R-1", "Starfall Chronicles", "Rsf", "zorblax nebulon galactic empire starship rebellion", 5);
        createBook("R-2", "Tidewater Kitchen", "Rck", "quillfruit sourdough baking pastry oven recipes", 5);
        createBook("R-3", "Sold Out Galaxy", "Rsf", "zorblax nebulon galactic empire starship rebellion", 0);

        mvc.perform(get("/api/v1/books/recommendations").param("query", "zorblax galactic starship rebellion").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].book.isbn").value("R-1"))
                .andExpect(jsonPath("$.results[0].score", greaterThan(0.5)))
                .andExpect(jsonPath("$.results[*].book.isbn", not(hasItem("R-3"))));
    }

    @Test
    void similarToExcludesTheSourceBookAndPicksTheClosestNeighbour() throws Exception {
        long src = createBook("S-1", "Quantum Gardens", "Ssc", "plimsoll quasar entanglement physics lattice", 5);
        createBook("S-2", "Lattice Physics", "Ssc", "plimsoll quasar entanglement physics lattice experiments", 5);
        createBook("S-3", "Pasta Poetry", "Scook", "fettuccine marinara basil simmer", 5);

        mvc.perform(get("/api/v1/books/recommendations").param("similarTo", String.valueOf(src)).param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].book.isbn").value("S-2"))
                .andExpect(jsonPath("$.results[*].book.id", not(hasItem((int) src))));
    }

    @Test
    void editedBookIsReEmbeddedAndDeletedBookDisappears() throws Exception {
        long id = createBook("E-1", "Shifting Title", "Ech", "wobblegrass meadow shepherd", 5);
        mvc.perform(get("/api/v1/books/recommendations").param("query", "wobblegrass meadow shepherd").param("limit", "1"))
                .andExpect(jsonPath("$.results[0].book.isbn").value("E-1"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/books/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isbn\":\"E-1\",\"title\":\"Shifting Title\",\"author\":\"A. Writer\",\"genre\":\"Ech\","
                        + "\"description\":\"driftwood lighthouse harbour tempest\",\"price\":10,\"stock\":5}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/books/recommendations").param("query", "driftwood lighthouse harbour tempest").param("limit", "1"))
                .andExpect(jsonPath("$.results[0].book.isbn").value("E-1"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/books/{id}", id))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/books/recommendations").param("query", "driftwood lighthouse harbour tempest").param("limit", "20"))
                .andExpect(jsonPath("$.results[*].book.isbn", not(hasItem("E-1"))));
    }

    @Test
    void badInputsAreRejectedClearly() throws Exception {
        mvc.perform(get("/api/v1/books/recommendations")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/books/recommendations").param("query", "x").param("similarTo", "1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/books/recommendations").param("similarTo", "987654"))
                .andExpect(status().isNotFound());
    }
}
