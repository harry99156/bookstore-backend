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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end API tests over the real Spring context + H2: the full browse -> cart -> checkout -> cancel journey. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookstoreApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private long createBook(String isbn, String title, String genre, String price, int stock) throws Exception {
        String body = """
                {"isbn":"%s","title":"%s","author":"Test Author","genre":"%s","description":"d","price":%s,"stock":%d}
                """.formatted(isbn, title, genre, price, stock);
        String res = mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/books/")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    private String createCart() throws Exception {
        String res = mvc.perform(post("/api/v1/carts")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asText();
    }

    private void addToCart(String cartId, long bookId, int qty) throws Exception {
        mvc.perform(post("/api/v1/carts/{c}/items", cartId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":%d,\"quantity\":%d}".formatted(bookId, qty))).andExpect(status().isOk());
    }

    private String checkoutBody(String cartId, String email) {
        return """
                {"cartId":"%s","customerName":"Ada","customerEmail":"%s","shippingAddress":"1 Main St"}
                """.formatted(cartId, email);
    }

    @Test
    void fullPurchaseJourneyDecrementsStockAndCancelRestocks() throws Exception {
        long bookId = createBook("T-0001", "Journey Book", "Journey", "10.00", 5);
        String cartId = createCart();
        addToCart(cartId, bookId, 2);
        addToCart(cartId, bookId, 1); // merges into a single line of 3

        mvc.perform(get("/api/v1/carts/{c}", cartId))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(30.00));

        String orderJson = mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId, "ada@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(30.00))
                .andReturn().getResponse().getContentAsString();
        long orderId = json.readTree(orderJson).get("id").asLong();

        mvc.perform(get("/api/v1/books/{id}", bookId)).andExpect(jsonPath("$.stock").value(2));
        mvc.perform(get("/api/v1/carts/{c}", cartId)).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/v1/orders").param("email", "ADA@example.com"))
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/v1/books/{id}", bookId)).andExpect(jsonPath("$.stock").value(5));

        mvc.perform(post("/api/v1/orders/{id}/cancel", orderId)).andExpect(status().isConflict());
    }

    @Test
    void cannotAddMoreThanStockAndCannotCheckoutEmptyCart() throws Exception {
        long bookId = createBook("T-0002", "Scarce Book", "Scarce", "5.00", 2);
        String cartId = createCart();

        mvc.perform(post("/api/v1/carts/{c}/items", cartId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":%d,\"quantity\":3}".formatted(bookId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("in stock")));

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId, "x@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cart is empty"));
    }

    @Test
    void checkoutFailsAtomicallyWhenStockDisappearsAfterCarting() throws Exception {
        long bookId = createBook("T-0003", "Racy Book", "Racy", "7.00", 2);
        String cartId = createCart();
        addToCart(cartId, bookId, 2);

        // Another shopper buys everything first.
        String other = createCart();
        addToCart(other, bookId, 2);
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody(other, "other@example.com"))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId, "late@example.com")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/books/{id}", bookId)).andExpect(jsonPath("$.stock").value(0));
        mvc.perform(get("/api/v1/orders").param("email", "late@example.com")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchFiltersSortsAndPaginates() throws Exception {
        createBook("T-0010", "Zeta Search", "SearchGenre", "30.00", 1);
        createBook("T-0011", "Alpha Search", "SearchGenre", "10.00", 1);
        createBook("T-0012", "Mid Search", "SearchGenre", "20.00", 1);

        mvc.perform(get("/api/v1/books").param("genre", "searchgenre").param("sortBy", "price")
                        .param("direction", "desc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Zeta Search"));

        mvc.perform(get("/api/v1/books").param("q", "alpha search").param("maxPrice", "15"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isbn").value("T-0011"));
    }

    @Test
    void validationAndNotFoundUseProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\",\"title\":\"x\",\"author\":\"a\",\"genre\":\"g\",\"price\":-1,\"stock\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.isbn").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists());

        mvc.perform(get("/api/v1/books/{id}", 999999)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("not found")));

        createBook("T-0020", "Dup", "Dup", "1.00", 1);
        mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"T-0020\",\"title\":\"x\",\"author\":\"a\",\"genre\":\"g\",\"price\":1,\"stock\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAndDeleteBook() throws Exception {
        long id = createBook("T-0030", "Old Title", "Edit", "3.00", 1);
        mvc.perform(put("/api/v1/books/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"T-0030\",\"title\":\"New Title\",\"author\":\"a\",\"genre\":\"Edit\",\"price\":4.50,\"stock\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.price").value(4.50));
        mvc.perform(delete("/api/v1/books/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/books/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void healthAndOpenApiDocsAreExposed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists());
    }
}
