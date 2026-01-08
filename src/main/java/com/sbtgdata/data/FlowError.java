package com.sbtgdata.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "errors")
public class FlowError {

    @Id
    private String id;

    private String message;

    @org.springframework.data.mongodb.core.mapping.Field("flow_id")
    private String flowId;

    private LocalDateTime date;

    public FlowError() {
    }

    public FlowError(String message, String flowId) {
        this.message = message;
        this.flowId = flowId;
        this.date = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
