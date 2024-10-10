package ru.vladislav.baksahsnkij.product.listener;

import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.vladislav.baksahsnkij.product.model.Product;

import java.util.UUID;

@Component
public record ProductListener(KafkaTemplate<String, Object> kafkaTemplate) implements AfterSaveCallback<Product> {
    @Override
    public Product onAfterSave(Product entity, org.bson.Document document, String collection) {
        kafkaTemplate.send(
                "change",
                UUID.randomUUID().toString(),
                entity
        ).join();
        return entity;
    }
}
