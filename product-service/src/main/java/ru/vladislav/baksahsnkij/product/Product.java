package ru.vladislav.baksahsnkij.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
record Product(@Id Long id, String name) {

}
