package ru.vladislav.baksahsnkij.analytic.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.vladislav.baksahsnkij.analytic.model.Change;

public interface ChangeRepository extends MongoRepository<Change, String> {

}
