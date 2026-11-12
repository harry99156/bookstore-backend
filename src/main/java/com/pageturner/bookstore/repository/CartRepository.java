package com.pageturner.bookstore.repository;

import com.pageturner.bookstore.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, String> {
}
