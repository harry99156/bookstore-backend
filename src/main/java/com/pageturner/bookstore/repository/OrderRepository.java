package com.pageturner.bookstore.repository;

import com.pageturner.bookstore.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
