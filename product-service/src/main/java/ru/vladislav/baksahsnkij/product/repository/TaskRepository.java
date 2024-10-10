package ru.vladislav.baksahsnkij.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.vladislav.baksahsnkij.product.model.Task;

public interface TaskRepository extends MongoRepository<Task, String> {
}
