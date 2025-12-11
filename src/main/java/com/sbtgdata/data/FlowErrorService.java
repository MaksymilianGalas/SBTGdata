package com.sbtgdata.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowErrorService {
    
    @Autowired
    private FlowErrorRepository flowErrorRepository;
    
    public List<FlowError> getErrorsByFlowId(String flowId) {
        return flowErrorRepository.findByFlowId(flowId);
    }
    
    public void deleteError(String errorId) {
        flowErrorRepository.deleteById(errorId);
    }
    
    public void deleteAllErrorsByFlowId(String flowId) {
        flowErrorRepository.deleteByFlowId(flowId);
    }
}

