package ru.vladislav.baksahsnkij.order.integration.customer;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CustomerService {
    private final CustomerClient customerClient;

    public CustomerService(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @Cacheable(cacheNames = "customers", key = "#root.args")
    public Customer getById(Long id) {
        return customerClient.getById(id);
    }
}
