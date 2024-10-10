package ru.vladislav.baksahsnkij.product.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public record Product(@Id Long id, String name) {

}
