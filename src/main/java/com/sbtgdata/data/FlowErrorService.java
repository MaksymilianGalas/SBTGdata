package com.sbtgdata.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FlowErrorService {

    @Autowired
    private FlowErrorRepository flowErrorRepository;

    @Autowired
    private DataFlowRepository dataFlowRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${external.error.delete.webhook.url:}")
    private String errorDeleteWebhookEndpoint;

    public List<FlowError> getErrorsByFlowId(String flowId) {
        return flowErrorRepository.findByFlowId(flowId);
    }

    public List<FlowError> getUniqueErrorsByFlowId(String flowId) {
        List<FlowError> allErrors = flowErrorRepository.findByFlowId(flowId);
        List<FlowError> uniqueErrors = new ArrayList<>();
        Set<String> seenMessages = new HashSet<>();

        for (FlowError error : allErrors) {
            if (error.getMessage() != null && !seenMessages.contains(error.getMessage())) {
                seenMessages.add(error.getMessage());
                uniqueErrors.add(error);
            }
        }

        return uniqueErrors;
    }

    public List<FlowError> getErrorsByUserId(String userId) {
        try {
            org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(userId);
            List<String> userFlowIds = dataFlowRepository.findByUserId(objectId).stream()
                    .map(DataFlow::getId)
                    .collect(Collectors.toList());

            return flowErrorRepository.findAll().stream()
                    .filter(error -> userFlowIds.contains(error.getFlowId()))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }

    public void deleteError(String errorId) {
        flowErrorRepository.findById(errorId).ifPresent(error -> {
            notifyExternalOnDelete(error);
            flowErrorRepository.deleteById(errorId);
        });
    }

    public void deleteAllErrorsByFlowId(String flowId) {
        flowErrorRepository.deleteByFlowId(flowId);
    }

    private void notifyExternalOnDelete(FlowError error) {
        if (errorDeleteWebhookEndpoint == null || errorDeleteWebhookEndpoint.isBlank()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("error_id", error.getId());
        payload.put("flow_id", error.getFlowId());
        payload.put("message", error.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(errorDeleteWebhookEndpoint, request, String.class);
    }
}
