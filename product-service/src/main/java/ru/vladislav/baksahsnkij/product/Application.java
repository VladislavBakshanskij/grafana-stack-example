package ru.vladislav.baksahsnkij.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpProperties;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    @Bean
    public MongoClientSettingsBuilderCustomizer mongoMetricsSynchronousContextProvider(ObservationRegistry registry) {
        return clientSettingsBuilder -> clientSettingsBuilder.contextProvider(ContextProviderFactory.create(registry))
                .addCommandListener(new MongoObservationCommandListener(registry));
    }

    @Bean
    public SpanProcessor hostnameSpanProcessor(@Value("${hostname}") String hostname) {
        return new HostnameSpanProcessor(hostname);
    }

    @Bean
    public SpanProcessor baggageSpanProcessor(@Value("${management.tracing.baggage.correlation.fields}") List<String> tags) {
        return new BaggageTaggingSpanProcessor(tags);
    }

//    @Bean
//    public SpanProcessor batchSpanProcessor() {
//        return BatchSpanProcessor.builder(LoggingSpanExporter.create()).build();
//    }
//
//    @Bean
//    @ConditionalOnBean(OtlpTracingConnectionDetails.class)
//    @ConditionalOnEnabledTracing
//    OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpProperties properties,
//                                              OtlpTracingConnectionDetails connectionDetails) {
//        var compression = properties.getCompression();
//        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
//                .setEndpoint(connectionDetails.getUrl())
//                .setTimeout(properties.getTimeout())
//                .setCompression(Objects.toString(compression).toLowerCase())
//                .setProxy(ProxyOptions.create(new InetSocketAddress(
//                        "localhost",
//                        8088
//                )));
//        for (Map.Entry<String, String> header : properties.getHeaders().entrySet()) {
//            builder.addHeader(header.getKey(), header.getValue());
//        }
//        return builder.build();
//    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public OncePerRequestFilter oncePerRequestFilter(Tracer tracer) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                Span span = tracer.currentSpan();
                TraceContext context = span.context();
                String traceId = context.traceId();
                String spanId = context.spanId();
                boolean sampled = context.sampled();

                response.addHeader("traceparent", "00-%s-%s-%s".formatted(traceId, spanId, (sampled ? "01" : "00")));
                System.out.println();
                filterChain.doFilter(request, response);
            }
        };
    }


    @Bean
    public TaskDecorator taskDecorator(Tracer tracer) {
        return new CompositeTaskDecorator(List.of(
                new MdcToTraceTaskDecorator("", tracer),
                new ContextPropagatingTaskDecorator()
        ));
    }
//
//    @Bean
//    public SpanProcessor batchFileSpanProcessor() {
//        return BatchSpanProcessor.builder(fileSpanExporter())
//                .build();
//    }

    public SpanExporter fileSpanExporter() {
        return new SpanExporter() {
            private static final Logger logger = LoggerFactory.getLogger("my-logger");

            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    TraceRequestMarshaler traceRequestMarshaler = TraceRequestMarshaler.create(spans);
                    traceRequestMarshaler.writeJsonTo(out);

                    logger.info("{}", out);

                    return CompletableResultCode.ofSuccess();
                } catch (IOException e) {
                    e.printStackTrace();
                    return CompletableResultCode.ofFailure();
                }
            }

            @Override
            public CompletableResultCode flush() {
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofSuccess();
            }
        };
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
