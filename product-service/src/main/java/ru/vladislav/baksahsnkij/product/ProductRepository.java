package ru.vladislav.baksahsnkij.product;

import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductRepository extends MongoRepository<Product, Long> {
}
