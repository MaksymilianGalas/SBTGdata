package com.sbtgdata.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    private String name;
    private Set<String> allowedViews; // Stores fully qualified class names of allowed views

    public Role() {
        this.allowedViews = new HashSet<>();
    }

    public Role(String name) {
        this.name = name;
        this.allowedViews = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAllowedViews() {
        return allowedViews;
    }

    public void setAllowedViews(Set<String> allowedViews) {
        this.allowedViews = allowedViews;
    }
}
