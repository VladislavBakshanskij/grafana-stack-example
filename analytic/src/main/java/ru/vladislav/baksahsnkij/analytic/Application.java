package ru.vladislav.baksahsnkij.analytic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoMetricsSynchronousContextProvider(ObservationRegistry registry) {
        return clientSettingsBuilder -> clientSettingsBuilder.contextProvider(ContextProviderFactory.create(registry))
                .addCommandListener(new MongoObservationCommandListener(registry));
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
    public ConcurrentKafkaListenerContainerFactory<String, JsonNode> factory(KafkaProperties kafkaProperties,
                                                                             SslBundles sslBundles,
                                                                             ObjectMapper objectMapper) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, JsonNode>();
        JsonDeserializer<JsonNode> objectJsonDeserializer = new JsonDeserializer<>(JsonNode.class, objectMapper);
        objectJsonDeserializer.setUseTypeHeaders(false);
        objectJsonDeserializer.addTrustedPackages("*");
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(sslBundles),
                new StringDeserializer(),
                objectJsonDeserializer
        ));
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setObservationEnabled(true);
        return factory;
    }
}

@Document("changes")
record Change(@Id String id, JsonNode change) {

}

interface ChangeRepository extends MongoRepository<Change, String> {

}

@Component
record Listener(ObjectMapper objectMapper, ChangeRepository changeRepository) {
    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    @KafkaListener(topics = "change", containerFactory = "factory")
    public void onChange(Message<JsonNode> message) {
        JsonNode payload = message.getPayload();
        changeRepository.save(new Change(null, payload));
        log.info("{}", payload);
        log.info("COMPLETE");
    }
}
