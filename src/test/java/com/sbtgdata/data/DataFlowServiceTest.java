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

    @Mock
    private FlowErrorService flowErrorService;

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

        ReflectionTestUtils.setField(dataFlowService, "flowCreateWebhookEndpoint", "http://test-webhook1.com");
        ReflectionTestUtils.setField(dataFlowService, "flowCreateWebhookEndpoint2", "http://test-webhook2.com");
    }

    @Test
    void testSave_NewFlow_CallsWebhooks() {
        when(dataFlowRepository.save(any(DataFlow.class))).thenReturn(testFlow);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        DataFlow result = dataFlowService.save(testFlow);

        assertNotNull(result);
        verify(restTemplate, times(2)).postForEntity(anyString(), any(), any(Class.class));
    }

    @Test
    void testDelete_CallsWebhooks() {
        ReflectionTestUtils.setField(dataFlowService, "flowDeleteWebhookEndpoint", "http://test-delete.com");
        ReflectionTestUtils.setField(dataFlowService, "flowDeleteWebhookEndpoint2", "http://test-delete2.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        dataFlowService.delete(testFlow);

        verify(dataFlowRepository, times(1)).delete(testFlow);
        verify(restTemplate, times(2)).postForEntity(anyString(), any(), any(Class.class));
    }
}
