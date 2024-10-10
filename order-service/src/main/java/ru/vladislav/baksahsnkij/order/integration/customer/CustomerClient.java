package ru.vladislav.baksahsnkij.order.integration.customer;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("customers")
public interface CustomerClient {
    @GetExchange("{id}")
    Customer getById(@PathVariable Long id);
}
