package ru.vladislav.baksahsnkij.product;

import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
record ProductListener(KafkaTemplate<String, Object> kafkaTemplate) implements AfterSaveCallback<Product> {
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
