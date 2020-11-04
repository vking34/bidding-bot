package com.chozoi.auction_bot_v2.strategies.repositories;

import java.util.List;

import com.chozoi.auction_bot_v2.strategies.models.Stage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface StageRepository extends MongoRepository<Stage, String> {

    List<Stage> findByState(String state);
}