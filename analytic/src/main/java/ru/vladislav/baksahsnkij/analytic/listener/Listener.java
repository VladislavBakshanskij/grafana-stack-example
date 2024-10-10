package ru.vladislav.baksahsnkij.analytic.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import ru.vladislav.baksahsnkij.analytic.model.Change;
import ru.vladislav.baksahsnkij.analytic.repository.ChangeRepository;

@Component
public record Listener(ObjectMapper objectMapper, ChangeRepository changeRepository) {
    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    @KafkaListener(topics = "change", containerFactory = "factory")
    public void onChange(Message<JsonNode> message) {
        JsonNode payload = message.getPayload();
        changeRepository.save(new Change(null, payload));
        log.info("{}", payload);
        log.info("COMPLETE");
    }
}
