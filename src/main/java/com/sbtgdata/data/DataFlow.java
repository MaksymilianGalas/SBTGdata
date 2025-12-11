package com.sbtgdata.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "dataflows")
public class DataFlow {

    @Id
    private String id;

    private String name;
    private String ownerEmail;
    private Map<String, String> inputSchema;
    private String pythonCode;
    private String additionalLibraries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataFlow() {
        this.inputSchema = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Map<String, String> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, String> inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getPythonCode() {
        return pythonCode;
    }

    public void setPythonCode(String pythonCode) {
        this.pythonCode = pythonCode;
    }

    public String getAdditionalLibraries() {
        return additionalLibraries;
    }

    public void setAdditionalLibraries(String additionalLibraries) {
        this.additionalLibraries = additionalLibraries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
