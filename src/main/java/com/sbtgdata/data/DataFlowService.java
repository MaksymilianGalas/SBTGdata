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
    
    @Value("${external.flow.webhook.url2:}")
    private String flowWebhookEndpoint2;
    
    @Value("${external.flow.delete.webhook.url:}")
    private String flowDeleteWebhookEndpoint;
    
    @Value("${external.data.retrieval.webhook.url:}")
    private String dataRetrievalWebhookEndpoint;
    
    @Value("${external.flow.base.url:}")
    private String flowBaseUrl;

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
            dataFlow.setUserId(resolveOwnerId(dataFlow.getOwnerEmail()));
        }
        
        if (dataFlow.getPythonCode() != null && dataFlow.getFunction() == null) {
            dataFlow.setFunction(dataFlow.getPythonCode());
        }
        
        if (dataFlow.getAdditionalLibraries() != null && 
            (dataFlow.getPackages() == null || dataFlow.getPackages().isEmpty())) {
            String[] libs = dataFlow.getAdditionalLibraries().split("\n");
            dataFlow.setPackages(java.util.Arrays.asList(libs));
        }

        DataFlow saved = dataFlowRepository.save(dataFlow);

        if (isNew) {
            try {
                notifyExternalOnCreate(saved);
            } catch (RuntimeException ex) {
                if (saved.getId() != null) {
                    dataFlowRepository.deleteById(saved.getId());
                }
                errorEventPublisher.publish(null, saved.getId(), saved.getUserId(), ex.getMessage());
                throw new IllegalArgumentException("Zapis przepływu nieudany: " + ex.getMessage());
            }
        }

        return saved;
    }

    public void delete(DataFlow dataFlow) {
        try {
            notifyExternalOnDelete(dataFlow);
        } catch (RuntimeException ex) {
            errorEventPublisher.publish(null, dataFlow.getId(), dataFlow.getUserId(), ex.getMessage());
            throw new IllegalArgumentException("Usunięcie przepływu nieudane: " + ex.getMessage());
        }
        dataFlowRepository.delete(dataFlow);
    }

    public void deleteById(String id) {
        Optional<DataFlow> flowOpt = dataFlowRepository.findById(id);
        if (flowOpt.isPresent()) {
            delete(flowOpt.get());
        }
    }
    
    public String getFlowUrl(String flowId, String apiKey) {
        if (flowBaseUrl == null || flowBaseUrl.isBlank()) {
            return "";
        }
        return flowBaseUrl + "/" + flowId + "?api_key=" + apiKey;
    }
    
    public byte[] retrieveData(String flowId, long startDate, long endDate) {
        if (dataRetrievalWebhookEndpoint == null || dataRetrievalWebhookEndpoint.isBlank()) {
            throw new IllegalStateException("Brak skonfigurowanego endpointu pobierania danych");
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("flow_id", flowId);
        payload.put("start_date", startDate);
        payload.put("end_date", endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
            dataRetrievalWebhookEndpoint, request, byte[].class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Endpoint zwrócił status " + response.getStatusCode());
        }
        
        return response.getBody();
    }

    private void notifyExternalOnCreate(DataFlow flow) {
        String userId = flow.getUserId() != null ? flow.getUserId() : resolveOwnerId(flow.getOwnerEmail());
        
        if (flowWebhookEndpoint != null && !flowWebhookEndpoint.isBlank()) {
            Map<String, Object> payload1 = new HashMap<>();
            payload1.put("user_id", userId);
            payload1.put("flow_id", flow.getId());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(payload1, headers);
            
            ResponseEntity<String> response1 = restTemplate.postForEntity(flowWebhookEndpoint, request1, String.class);
            if (!response1.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 1 zwrócił status " + response1.getStatusCode());
            }
        }
        
        if (flowWebhookEndpoint2 != null && !flowWebhookEndpoint2.isBlank()) {
            Map<String, Object> payload2 = new HashMap<>();
            payload2.put("flow_id", flow.getId());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request2 = new HttpEntity<>(payload2, headers);
            
            ResponseEntity<String> response2 = restTemplate.postForEntity(flowWebhookEndpoint2, request2, String.class);
            if (!response2.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 2 zwrócił status " + response2.getStatusCode());
            }
        }
    }
    
    private void notifyExternalOnDelete(DataFlow flow) {
        String userId = flow.getUserId() != null ? flow.getUserId() : resolveOwnerId(flow.getOwnerEmail());
        
        if (flowDeleteWebhookEndpoint == null || flowDeleteWebhookEndpoint.isBlank()) {
            return;
        }
        
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("user_id", userId);
        payload1.put("flow_id", flow.getId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(payload1, headers);
        
        restTemplate.postForEntity(flowDeleteWebhookEndpoint, request1, String.class);
    }

    private String resolveOwnerId(String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .map(User::getId)
                .orElse(ownerEmail);
    }
}
