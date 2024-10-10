package ru.vladislav.baksahsnkij.product.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanName;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.ContinueSpan;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Service
public class SpecialService {
    private static final Logger log = LoggerFactory.getLogger(SpecialService.class);

    @Autowired
    RestClient.Builder restClientBuilder;

    @Autowired
    Tracer tracer;

    @NewSpan(name = "my_new_span", value = "my_new_span_value")
    public void newSpanWithAllFields() {
        Span span = tracer.currentSpan();
        span.event("REQUEST_STARTED", System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        log.info("newSpanWithAllFields");
        send();
        span.event("REQUEST_COMPLETED", System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @NewSpan(name = "my_new_span")
    public void newSpanWithName() {
        log.info("newSpanWithName");
        send();
    }

    @NewSpan(value = "my_new_span_value")
    public void newSpanWithValue() {
        log.info("newSpanWithValue");
        send();
    }

    @NewSpan
    public void newSpanWithDefault() {
        log.info("newSpanWithDefault");
        send();
    }

    @ContinueSpan
    public void continueSpan() {
        log.info("continueSpan");
        send();
    }

    @ContinueSpan(log = "continueSpanWithLog")
    public void continueSpanWithLog() {
        log.info("continueSpanWithLog");
        send();
    }

    @SpanName("span_spec_name")
    public void spanNameAndTag(@SpanTag(key = "span_spec_value") int value) {
        log.info("spanNameAndTag");
        send();
    }

    private void send() {
        String _ = restClientBuilder.build()
                .get()
                .uri("https://ifconfig.me")
                .retrieve()
                .body(String.class);
    }
}
