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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Value("${webhook.flow.create:}")
    private String flowCreateWebhookEndpoint;

    @Value("${webhook.flow.delete.endpoint1:}")
    private String flowDeleteEndpoint1;

    @Value("${webhook.flow.delete.endpoint2:}")
    private String flowDeleteEndpoint2;

    @Value("${external.data.base.url:}")
    private String dataBaseUrl;

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

    public String getDataRetrievalUrl(String flowId, String apiKey, LocalDateTime startDate, LocalDateTime endDate) {
        if (dataBaseUrl == null || dataBaseUrl.isBlank()) {
            throw new IllegalStateException("Brak skonfigurowanego bazowego URL do pobierania danych");
        }

        StringBuilder url = new StringBuilder();
        url.append(dataBaseUrl).append("/flows/").append(flowId).append("/data?API_KEY=").append(apiKey);

        if (startDate != null && endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);

            // URL encode dates (space becomes %20)
            String encodedStart = URLEncoder.encode(startDateStr, StandardCharsets.UTF_8);
            String encodedEnd = URLEncoder.encode(endDateStr, StandardCharsets.UTF_8);

            url.append("&start_date=").append(encodedStart);
            url.append("&end_date=").append(encodedEnd);
        }

        return url.toString();
    }

    private void notifyExternalOnCreate(DataFlow flow) {
        if (flowCreateWebhookEndpoint == null || flowCreateWebhookEndpoint.isBlank()) {
            throw new IllegalStateException("Brak skonfigurowanego endpointu webhook");
        }

        String userId = flow.getUserId() != null ? flow.getUserId() : resolveOwnerId(flow.getOwnerEmail());

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("flow_id", flow.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(flowCreateWebhookEndpoint, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Endpoint zwrócił status " + response.getStatusCode());
        }
    }

    private void notifyExternalOnDelete(DataFlow flow) {
        String userId = flow.getUserId() != null ? flow.getUserId() : resolveOwnerId(flow.getOwnerEmail());

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("flow_id", flow.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // Wyślij do pierwszego endpointu
        if (flowDeleteEndpoint1 != null && !flowDeleteEndpoint1.isBlank()) {
            ResponseEntity<String> response1 = restTemplate.postForEntity(flowDeleteEndpoint1, request, String.class);
            if (!response1.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 1 zwrócił status " + response1.getStatusCode());
            }
        }

        // Wyślij do drugiego endpointu
        if (flowDeleteEndpoint2 != null && !flowDeleteEndpoint2.isBlank()) {
            ResponseEntity<String> response2 = restTemplate.postForEntity(flowDeleteEndpoint2, request, String.class);
            if (!response2.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Endpoint 2 zwrócił status " + response2.getStatusCode());
            }
        }
    }

    private String resolveOwnerId(String ownerEmail) {
        return userRepository.findByEmail(ownerEmail)
                .map(User::getId)
                .orElse(ownerEmail);
    }
}
