package com.sbtgdata.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String email;
    private String password;
    private Set<String> roles;
    private String API_KEY;

    public User() {
    }

    public User(String email, String password, Set<String> roles) {
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public String getApiKey() {
        return API_KEY;
    }

    public void setApiKey(String apiKey) {
        this.API_KEY = apiKey;
    }
}
