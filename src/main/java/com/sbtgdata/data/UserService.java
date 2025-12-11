package com.sbtgdata.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sbtgdata.config.ErrorEventPublisher;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ErrorEventPublisher errorEventPublisher;

    @Value("${external.webhook.url:}")
    private String webhookEndpoint;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User registerUser(String email, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Użytkownik o podanym emailu już istnieje");
        }

        Set<String> roles = new HashSet<>();
        roles.add(role != null ? role : "USER");

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setApiKey(generateApiKey());

        User savedUser = userRepository.save(user);

        try {
            notifyExternal(savedUser);
        } catch (RuntimeException ex) {
            if (savedUser.getId() != null) {
                userRepository.deleteById(savedUser.getId());
            }
            errorEventPublisher.publish(null, null, savedUser.getId(), ex.getMessage());
            throw new IllegalArgumentException("Rejestracja nieudana: " + ex.getMessage());
        }

        return savedUser;
    }

    public boolean validateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return passwordEncoder.matches(password, user.getPassword());
    }

    public String regenerateApiKey(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Użytkownik nie istnieje");
        }
        
        User user = userOpt.get();
        String newApiKey = generateApiKey();
        user.setApiKey(newApiKey);
        userRepository.save(user);
        return newApiKey;
    }
    
    public String getApiKey(String userId) {
        return userRepository.findById(userId)
                .map(User::getApiKey)
                .orElse(null);
    }
    
    private String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void notifyExternal(User user) {
        if (webhookEndpoint == null || webhookEndpoint.isBlank()) {
            throw new IllegalStateException("Brak skonfigurowanego endpointu webhook");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(webhookEndpoint, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Endpoint zwrócił status " + response.getStatusCode());
        }
    }
}
