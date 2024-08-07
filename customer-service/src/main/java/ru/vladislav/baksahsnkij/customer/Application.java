package ru.vladislav.baksahsnkij.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.pyroscope.labels.LabelsSet;
import io.pyroscope.labels.Pyroscope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.vladislav.baksahsnkij.customer.ThrowingRunnable.throwingRunnable;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public SdkTracerProviderBuilderCustomizer sdkTracerProviderBuilderCustomizer(@Value("${hostname}") String hostname) {
        return builder -> builder.addSpanProcessor(new BaggageTaggingSpanProcessor(List.of("rqid")))
                .addSpanProcessor(new SpanProcessor() {
                    @Override
                    public void onStart(Context context, ReadWriteSpan readWriteSpan) {
                        readWriteSpan.setAttribute("hostname", hostname);
                    }

                    @Override
                    public boolean isStartRequired() {
                        return true;
                    }

                    @Override
                    public void onEnd(ReadableSpan readableSpan) {

                    }

                    @Override
                    public boolean isEndRequired() {
                        return false;
                    }
                });
    }


    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper objectMapper,
                                                       KafkaProperties kafkaProperties,
                                                       SslBundles sslBundles) {
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildConsumerProperties(sslBundles),
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        ));
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }
}

@Component
class AgentLabelFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public AgentLabelFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ThrowingRunnable<Exception> action = throwingRunnable(() -> filterChain.doFilter(request, response));
        Optional.of(tracer)
                .map(Tracer::currentSpan)
                .map(Span::context)
                .ifPresentOrElse(
                        context -> {
                            Map<String, String> labels = Map.of(
                                    "traceId", context.traceId(),
                                    "spanId", context.spanId()
                            );

                            Pyroscope.LabelsWrapper.run(new LabelsSet(labels), action);
                        },
                        action
                );

    }
}

interface ThrowingRunnable<E extends Throwable> extends Runnable {
    static <E extends Throwable> ThrowingRunnable<E> throwingRunnable(ThrowingRunnable<E> throwingRunnable) {
        return throwingRunnable;
    }

    void tryRun() throws E;

    @Override
    default void run() {
        try {
            tryRun();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}


record Customer(Long id, String name) {

}

record CreateCustomer(String name) {

}

@RestController
@RequestMapping("customers")
record CustomerController(JdbcClient jdbcClient, KafkaTemplate<String, Object> kafkaTemplate) {
    @PostMapping
    public Customer createCustomer(@RequestBody CreateCustomer customer) {
        long start = System.currentTimeMillis();
        long end;
        do {
            end = System.currentTimeMillis();
        } while (end - start < TimeUnit.SECONDS.toMillis(2));

        return jdbcClient.sql("""
                        insert into customers (name)
                        values (:name)
                        on conflict (name)
                        do update set update_datetime = now()
                        returning *
                        """)
                .param("name", customer.name())
                .query(Customer.class)
                .optional()
                .map(it -> {
                    kafkaTemplate.send(
                            "change",
                            UUID.randomUUID().toString(),
                            it
                    ).join();
                    return it;
                })
                .orElseThrow();
    }

    @GetMapping("{id}")
    public Customer getById(@PathVariable Long id) {
        return jdbcClient.sql("select * from customers where id = :id")
                .param("id", id)
                .query(Customer.class)
                .optional()
                .orElseThrow();
    }
}
