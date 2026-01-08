package com.sbtgdata.data;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sbtgdata.config.ErrorEventPublisher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataFlowService {

    @Autowired
    private DataFlowRepository dataFlowRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ErrorEventPublisher errorEventPublisher;

    @Autowired
    private FlowErrorService flowErrorService;

    @Value("${external.flow.create.webhook.url:}")
    private String flowCreateWebhookEndpoint;

    @Value("${external.flow.create.webhook.url2:}")
    private String flowCreateWebhookEndpoint2;

    @Value("${external.flow.delete.webhook.url:}")
    private String flowDeleteWebhookEndpoint;

    @Value("${external.flow.delete.webhook.url2:}")
    private String flowDeleteWebhookEndpoint2;

    @Value("${external.flow.start.webhook.url:}")
    private String flowStartWebhookEndpoint;

    @Value("${external.flow.stop.webhook.url:}")
    private String flowStopWebhookEndpoint;

    public List<DataFlow> findByOwnerEmail(String ownerEmail) {
        return dataFlowRepository.findByOwnerEmail(ownerEmail);
    }

    public List<DataFlow> findAll() {
        return dataFlowRepository.findAll();
    }

    public Optional<DataFlow> findById(String id) {
        return dataFlowRepository.findById(id);
    }

    public DataFlow save(DataFlow dataFlow) {
        boolean isNew = dataFlow.getId() == null;

        dataFlow.setUpdatedAt(LocalDateTime.now());

        if (dataFlow.getUserId() == null && dataFlow.getOwnerEmail() != null) {
            dataFlow.setUserIdFromString(resolveOwnerId(dataFlow.getOwnerEmail()));
        }

        DataFlow saved = dataFlowRepository.save(dataFlow);

        if (isNew) {
            try {
                notifyExternalOnCreate(saved);
            } catch (RuntimeException ex) {
                if (saved.getId() != null) {
                    dataFlowRepository.deleteById(saved.getId());
                }
                errorEventPublisher.publish(null, saved.getId(), saved.getUserIdAsString(), ex.getMessage());
                throw new IllegalArgumentException("Zapis przepływu nieudany: " + ex.getMessage());
            }
        }

        return saved;
    }

    public void delete(DataFlow dataFlow) {
        try {
            notifyExternalOnDelete(dataFlow);
        } catch (RuntimeException ex) {
            errorEventPublisher.publish(null, dataFlow.getId(), dataFlow.getUserIdAsString(), ex.getMessage());
            throw new IllegalArgumentException("Usunięcie przepływu nieudane: " + ex.getMessage());
        }
        flowErrorService.deleteAllErrorsByFlowId(dataFlow.getId());
        dataFlowRepository.delete(dataFlow);
    }

    public void deleteById(String id) {
        Optional<DataFlow> flowOpt = dataFlowRepository.findById(id);
        if (flowOpt.isPresent()) {
            delete(flowOpt.get());
        }
    }

    public void startFlow(String flowId) {
        Optional<DataFlow> flowOpt = dataFlowRepository.findById(flowId);
        if (flowOpt.isEmpty()) {
            throw new IllegalArgumentException("Przepływ nie istnieje");
        }

        DataFlow flow = flowOpt.get();
        flow.setStatus("RUNNING");
        flow.setUpdatedAt(LocalDateTime.now());
        dataFlowRepository.save(flow);

        notifyExternalOnStart(flow);
    }

    public void stopFlow(String flowId) {
        Optional<DataFlow> flowOpt = dataFlowRepository.findById(flowId);
        if (flowOpt.isEmpty()) {
            throw new IllegalArgumentException("Przepływ nie istnieje");
        }

        DataFlow flow = flowOpt.get();
        flow.setStatus("STOPPED");
        flow.setUpdatedAt(LocalDateTime.now());
        dataFlowRepository.save(flow);

        notifyExternalOnStop(flow);
    }

    private void notifyExternalOnCreate(DataFlow flow) {
        String userId = flow.getUserIdAsString() != null ? flow.getUserIdAsString()
                : resolveOwnerId(flow.getOwnerEmail());

        if (flowCreateWebhookEndpoint != null && !flowCreateWebhookEndpoint.isBlank()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", userId);
            payload.put("flow_id", flow.getId());
            payload.put("function", flow.getFunction());
            payload.put("packages", flow.getPackages());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flowCreateWebhookEndpoint, request,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 1 zwrócił status " + response.getStatusCode());
            }
        }

        if (flowCreateWebhookEndpoint2 != null && !flowCreateWebhookEndpoint2.isBlank()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("flow_id", flow.getId());
            payload.put("function", flow.getFunction());
            payload.put("packages", flow.getPackages());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flowCreateWebhookEndpoint2, request,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 2 zwrócił status " + response.getStatusCode());
            }
        }
    }

    private void notifyExternalOnDelete(DataFlow flow) {
        String userId = flow.getUserIdAsString() != null ? flow.getUserIdAsString()
                : resolveOwnerId(flow.getOwnerEmail());

        if (flowDeleteWebhookEndpoint != null && !flowDeleteWebhookEndpoint.isBlank()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", userId);
            payload.put("flow_id", flow.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(flowDeleteWebhookEndpoint, request, String.class);
        }

        if (flowDeleteWebhookEndpoint2 != null && !flowDeleteWebhookEndpoint2.isBlank()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("flow_id", flow.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(flowDeleteWebhookEndpoint2, request, String.class);
        }
    }

    private void notifyExternalOnStart(DataFlow flow) {
        if (flowStartWebhookEndpoint == null || flowStartWebhookEndpoint.isBlank()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("flow_id", flow.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(flowStartWebhookEndpoint, request, String.class);
    }

    private void notifyExternalOnStop(DataFlow flow) {
        if (flowStopWebhookEndpoint == null || flowStopWebhookEndpoint.isBlank()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("flow_id", flow.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(flowStopWebhookEndpoint, request, String.class);
    }

    private String resolveOwnerId(String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .map(User::getId)
                .orElse(ownerEmail);
    }
}
