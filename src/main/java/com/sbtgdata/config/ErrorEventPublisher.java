package com.sbtgdata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ErrorEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ErrorEventPublisher.class);

    public void publish(String requestId, String flowId, String userId, String errorMessage) {
        logger.error("Error occurred - RequestId: {}, FlowId: {}, UserId: {}, Message: {}",
                requestId, flowId, userId, errorMessage);
    }
}
