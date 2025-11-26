package com.sbtgdata.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DataFlowRepository extends MongoRepository<DataFlow, String> {
    List<DataFlow> findByOwnerEmail(String ownerEmail);
}
