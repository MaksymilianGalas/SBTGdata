package com.sbtgdata.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DataFlowService {

    @Autowired
    private DataFlowRepository dataFlowRepository;

    public List<DataFlow> findByOwnerEmail(String ownerEmail) {
        return dataFlowRepository.findByOwnerEmail(ownerEmail);
    }

    public List<DataFlow> findAll() {
        return dataFlowRepository.findAll();
    }

    public Optional<DataFlow> findById(String id) {
        return dataFlowRepository.findById(id);
    }

    public DataFlow save(DataFlow dataFlow) {
        dataFlow.setUpdatedAt(LocalDateTime.now());
        return dataFlowRepository.save(dataFlow);
    }

    public void delete(DataFlow dataFlow) {
        dataFlowRepository.delete(dataFlow);
    }

    public void deleteById(String id) {
        dataFlowRepository.deleteById(id);
    }
}
