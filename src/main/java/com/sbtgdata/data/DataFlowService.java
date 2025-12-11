package com.sbtgdata.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sbtgdata.config.ErrorEventPublisher;

import java.time.Instant;
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

    @Value("${external.flow.webhook.url:}")
    private String flowWebhookEndpoint;

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
        DataFlow saved = dataFlowRepository.save(dataFlow);

        if (isNew) {
            try {
                notifyExternalOnCreate(saved);
            } catch (RuntimeException ex) {
                if (saved.getId() != null) {
                    dataFlowRepository.deleteById(saved.getId());
                }
                errorEventPublisher.publish(null, saved.getId(), resolveOwnerId(saved.getOwnerEmail()), ex.getMessage());
                throw new IllegalArgumentException("Zapis przepływu nieudany: " + ex.getMessage());
            }
        }

        return saved;
    }

    public void delete(DataFlow dataFlow) {
        dataFlowRepository.delete(dataFlow);
    }

    public void deleteById(String id) {
        dataFlowRepository.deleteById(id);
    }

    private void notifyExternalOnCreate(DataFlow flow) {
        if (flowWebhookEndpoint == null || flowWebhookEndpoint.isBlank()) {
            throw new IllegalStateException("Brak skonfigurowanego endpointu webhook przepływu");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("flow_id", flow.getId());
        payload.put("user_id", resolveOwnerId(flow.getOwnerEmail()));
        payload.put("data_czas", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(flowWebhookEndpoint, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Endpoint zwrócił status " + response.getStatusCode());
        }
    }

    private String resolveOwnerId(String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .map(User::getId)
                .orElse(ownerEmail);
    }
}
