package com.sbtgdata.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "flows")
public class DataFlow {

    @Id
    private String id;

    private String name;
    private String ownerEmail;

    @Field("user_id")
    private ObjectId userId;

    private String function;
    private java.util.List<String> packages;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataFlow() {
        this.status = "STOPPED";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public void setUserIdFromString(String userIdString) {
        if (userIdString != null && !userIdString.isEmpty()) {
            this.userId = new ObjectId(userIdString);
        }
    }

    public String getUserIdAsString() {
        return userId != null ? userId.toHexString() : null;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public java.util.List<String> getPackages() {
        return packages;
    }

    public void setPackages(java.util.List<String> packages) {
        this.packages = packages;
    }
}
