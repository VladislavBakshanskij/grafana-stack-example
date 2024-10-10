package ru.vladislav.baksahsnkij.customer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {
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
