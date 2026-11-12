package com.pageturner.bookstore.service;

import com.pageturner.bookstore.exception.BusinessRuleException;
import com.pageturner.bookstore.exception.ResourceNotFoundException;
import com.pageturner.bookstore.model.Book;
import com.pageturner.bookstore.model.Cart;
import com.pageturner.bookstore.model.CartItem;
import com.pageturner.bookstore.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final BookService books;

    public CartService(CartRepository carts, BookService books) {
        this.carts = carts;
        this.books = books;
    }

    public Cart create() {
        return carts.save(new Cart());
    }

    @Transactional(readOnly = true)
    public Cart get(String id) {
        return carts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cart " + id + " not found"));
    }

    public Cart addItem(String cartId, Long bookId, int quantity) {
        Cart cart = get(cartId);
        Book book = books.get(bookId);
        int already = cart.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId)).mapToInt(CartItem::getQuantity).sum();
        requireStock(book, already + quantity);
        cart.addOrIncrease(book, quantity);
        return cart;
    }

    public Cart updateItem(String cartId, Long bookId, int quantity) {
        Cart cart = get(cartId);
        CartItem item = findItem(cart, bookId);
        requireStock(item.getBook(), quantity);
        item.setQuantity(quantity);
        return cart;
    }

    public Cart removeItem(String cartId, Long bookId) {
        Cart cart = get(cartId);
        cart.getItems().remove(findItem(cart, bookId));
        return cart;
    }

    private CartItem findItem(Cart cart, Long bookId) {
        return cart.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Book " + bookId + " is not in cart " + cart.getId()));
    }

    private void requireStock(Book book, int wanted) {
        if (wanted > book.getStock()) {
            throw new BusinessRuleException("Only " + book.getStock() + " copies of '" + book.getTitle() + "' in stock");
        }
    }
}
