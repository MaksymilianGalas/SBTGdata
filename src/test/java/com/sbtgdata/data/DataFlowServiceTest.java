package com.sbtgdata.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import com.sbtgdata.config.ErrorEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataFlowServiceTest {

    @Mock
    private DataFlowRepository dataFlowRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ErrorEventPublisher errorEventPublisher;

    @InjectMocks
    private DataFlowService dataFlowService;

    private DataFlow testFlow;
    private User testUser;

    @BeforeEach
    void setUp() {
        testFlow = new DataFlow();
        testFlow.setId("test-flow-id");
        testFlow.setName("Test Flow");
        testFlow.setOwnerEmail("test@example.com");
        
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        
        ReflectionTestUtils.setField(dataFlowService, "flowWebhookEndpoint", "http://test-webhook1.com");
        ReflectionTestUtils.setField(dataFlowService, "flowWebhookEndpoint2", "http://test-webhook2.com");
        ReflectionTestUtils.setField(dataFlowService, "flowBaseUrl", "http://test-base.com");
    }

    @Test
    void testSave_NewFlow_CallsWebhooks() {
        // Given
        when(dataFlowRepository.save(any(DataFlow.class))).thenReturn(testFlow);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        // When
        DataFlow result = dataFlowService.save(testFlow);

        // Then
        assertNotNull(result);
        verify(restTemplate, times(2)).postForEntity(anyString(), any(), any(Class.class));
    }

    @Test
    void testGetFlowUrl() {
        // Given
        String flowId = "test-flow-id";
        String apiKey = "test-api-key";

        // When
        String url = dataFlowService.getFlowUrl(flowId, apiKey);

        // Then
        assertEquals("http://test-base.com/test-flow-id?api_key=test-api-key", url);
    }

    @Test
    void testDelete_CallsWebhook() {
        // Given
        ReflectionTestUtils.setField(dataFlowService, "flowDeleteWebhookEndpoint", "http://test-delete.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        // When
        dataFlowService.delete(testFlow);

        // Then
        verify(dataFlowRepository, times(1)).delete(testFlow);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), any(Class.class));
    }
}

