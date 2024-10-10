package ru.vladislav.baksahsnkij.order.model.db;

import ru.vladislav.baksahsnkij.order.integration.customer.Customer;
import ru.vladislav.baksahsnkij.order.integration.product.Product;

public record Order(Long id, Product product, Customer customer) {
}
