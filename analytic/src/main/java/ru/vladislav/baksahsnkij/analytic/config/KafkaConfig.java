package ru.vladislav.baksahsnkij.analytic.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConfig {

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
