package com.sbtgdata.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowErrorRepository extends MongoRepository<FlowError, String> {
    List<FlowError> findByFlowId(String flowId);
    void deleteByFlowId(String flowId);
}

