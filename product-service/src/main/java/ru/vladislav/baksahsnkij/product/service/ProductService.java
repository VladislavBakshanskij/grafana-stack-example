package ru.vladislav.baksahsnkij.product.service;

import ru.vladislav.baksahsnkij.product.model.Product;

public interface ProductService {
    Product create(Product product);

    Product get( Long id);
}
