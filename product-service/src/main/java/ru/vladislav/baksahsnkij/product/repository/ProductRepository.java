package ru.vladislav.baksahsnkij.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.vladislav.baksahsnkij.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, Long> {
}
