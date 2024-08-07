package ru.vladislav.baksahsnkij.product;

import org.slf4j.MDC;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Document(collection = "tasks")
public class Task {
    @Id
    private String id;

    @Field("mdc_context")
    private Map<String, String> mdc;

    public Task() {
        mdc = MDC.getCopyOfContextMap();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getMdc() {
        return mdc;
    }

    public void setMdc(Map<String, String> mdc) {
        this.mdc = mdc;
    }
}
