package com.sbtgdata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class ErrorEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ErrorEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ErrorEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${kafka.error-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String data, String flowId, String userId, String blad) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        payload.put("flow_id", flowId);
        payload.put("user_id", userId);
        payload.put("blad", blad);
        payload.put("timestamp", Instant.now().toString());

        try {
            kafkaTemplate.send(topic, payload);
        } catch (Exception e) {
            logger.error("Kafka publish failed", e);
        }
    }
}

