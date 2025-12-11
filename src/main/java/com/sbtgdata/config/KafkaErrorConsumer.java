package com.sbtgdata.config;

import com.sbtgdata.data.ErrorNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KafkaErrorConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorConsumer.class);
    
    private final ErrorNotificationService errorNotificationService;

    public KafkaErrorConsumer(ErrorNotificationService errorNotificationService) {
        this.errorNotificationService = errorNotificationService;
    }

    @KafkaListener(topics = "${kafka.error-topic:errors}", groupId = "sbtgdata-error-consumer")
    public void consumeError(Map<String, Object> errorMessage) {
        logger.info("Received error from Kafka: {}", errorMessage);
        
        String blad = (String) errorMessage.get("blad");
        String flowId = (String) errorMessage.get("flow_id");
        String userId = (String) errorMessage.get("user_id");
        
        String message = "Błąd: " + (blad != null ? blad : "Nieznany błąd");
        if (flowId != null) {
            message += " (Flow ID: " + flowId + ")";
        }
        
        errorNotificationService.addError(userId, message);
    }
}

