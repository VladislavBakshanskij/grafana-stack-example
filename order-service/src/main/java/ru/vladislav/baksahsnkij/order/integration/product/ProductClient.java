package ru.vladislav.baksahsnkij.order.integration.product;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("products")
public interface ProductClient {
    @GetExchange("{id}")
    Product getById(@PathVariable Long id);
}
