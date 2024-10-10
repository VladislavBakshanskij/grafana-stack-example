package ru.vladislav.baksahsnkij.customer.repository.impl;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.vladislav.baksahsnkij.customer.model.db.Customer;
import ru.vladislav.baksahsnkij.customer.repository.CustomerRepository;

import java.util.Optional;

@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private final JdbcClient jdbcClient;

    public CustomerRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Customer> findById(long id) {
        return jdbcClient.sql("select * from customers where id = :id")
                .param("id", id)
                .query(Customer.class)
                .optional();
    }

    @Override
    public Customer save(Customer customer) {
        return jdbcClient.sql("""
                        insert into customers (name)
                        values (:name)
                        on conflict (name)
                        do update set update_datetime = now()
                        returning *
                        """)
                .param("name", customer.name())
                .query(Customer.class)
                .optional()
                .orElseThrow();
    }
}
