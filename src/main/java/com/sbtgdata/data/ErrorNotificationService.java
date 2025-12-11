package com.sbtgdata.data;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ErrorNotificationService {
    
    private final Map<String, Queue<String>> userErrors = new ConcurrentHashMap<>();
    
    public void addError(String userId, String errorMessage) {
        if (userId != null) {
            userErrors.computeIfAbsent(userId, k -> new LinkedList<>()).offer(errorMessage);
        }
    }
    
    public List<String> getAndClearErrors(String userId) {
        Queue<String> errors = userErrors.get(userId);
        if (errors == null || errors.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<>(errors);
        errors.clear();
        return result;
    }
    
    public boolean hasErrors(String userId) {
        Queue<String> errors = userErrors.get(userId);
        return errors != null && !errors.isEmpty();
    }
}

