package com.sbtgdata.config;

import com.sbtgdata.data.User;
import com.sbtgdata.data.UserService;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationContext authenticationContext;
    
    public void logout() {
        authenticationContext.logout();
    }
    
    public Optional<String> getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
            .map(org.springframework.security.core.userdetails.UserDetails::getUsername);
    }
    
    public boolean hasRole(String role) {
        Optional<org.springframework.security.core.userdetails.UserDetails> userDetailsOpt = 
            authenticationContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class);
        
        if (userDetailsOpt.isEmpty()) {
            return false;
        }
        
        org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsOpt.get();
        return userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }
    
    public User getCurrentUser() {
        Optional<String> emailOpt = getAuthenticatedUser();
        if (emailOpt.isPresent()) {
            return userService.findByEmail(emailOpt.get()).orElse(null);
        }
        return null;
    }
}

