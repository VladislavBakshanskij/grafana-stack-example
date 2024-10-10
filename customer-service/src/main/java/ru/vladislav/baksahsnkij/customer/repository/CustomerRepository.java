package ru.vladislav.baksahsnkij.customer.repository;

import ru.vladislav.baksahsnkij.customer.model.db.Customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(long id);

    Customer save(Customer customer);
}
