package ru.vladislav.baksahsnkij.analytic.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("changes")
public record Change(@Id String id, JsonNode change) {

}
