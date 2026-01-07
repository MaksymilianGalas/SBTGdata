package com.sbtgdata.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import com.sbtgdata.config.ErrorEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ErrorEventPublisher errorEventPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
    }

    @Test
    void testRegisterUser_GeneratesApiKey() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("new-user-id");
            return user;
        });
        when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("OK"));

        // When
        User result = userService.registerUser("test@example.com", "password", "USER");

        // Then
        assertNotNull(result);
        assertNotNull(result.getApiKey());
        assertFalse(result.getApiKey().isEmpty());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegenerateApiKey() {
        // Given
        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        String newApiKey = userService.regenerateApiKey("test-user-id");

        // Then
        assertNotNull(newApiKey);
        assertFalse(newApiKey.isEmpty());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testRegenerateApiKey_UserNotFound() {
        // Given
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.regenerateApiKey("non-existent");
        });
    }

    @Test
    void testGetApiKey() {
        // Given
        testUser.setApiKey("test-api-key");
        when(userRepository.findById("test-user-id")).thenReturn(Optional.of(testUser));

        // When
        String apiKey = userService.getApiKey("test-user-id");

        // Then
        assertEquals("test-api-key", apiKey);
    }
}

