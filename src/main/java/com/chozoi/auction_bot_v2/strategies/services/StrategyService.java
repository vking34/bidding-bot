package com.chozoi.auction_bot_v2.strategies.services;

import com.chozoi.auction_bot_v2.strategies.dto.AuctionStage;
import com.chozoi.auction_bot_v2.strategies.models.Stage;
import com.chozoi.auction_bot_v2.strategies.repositories.StageRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class StrategyService {

    @Getter
    private List<Stage> stages;

    private float proportionTotal;

    enum State {
        PUBLIC
    }

    @Autowired
    private StageRepository stageRepository;

    @PostConstruct
    private void loadStrategy(){
        // ! TODO: load strategy by calling api
        log.warn("Loading stages...");
        stages = stageRepository.findByState(State.PUBLIC.name());
        this.proportionTotal = (float) stages.stream().mapToDouble(Stage::getProportionTime).sum();
        stages.sort(Comparator.comparingInt(Stage::getSort));
        log.debug("Stages loaded: {}", stages);
        log.warn("proportionTotal: {}", proportionTotal);
    }

    public Optional<AuctionStage> getCurrentStage(long startTime, long endTime, long now){
        List<AuctionStage> auctionStages = calculateAuctionStages(startTime, endTime);
        return getCurrentStage(now, auctionStages);
    }

    public Optional<AuctionStage> getCurrentStage(long now, List<AuctionStage> auctionStages){
        for (AuctionStage auctionStage : auctionStages) {
            if (now >= auctionStage.getStartTime() && now < auctionStage.getEndTime()) {
                return Optional.of(auctionStage);
            }
        }

        return Optional.empty();
    }

    public Optional<AuctionStage> getNextStage(long now, List<AuctionStage> auctionStages){
        Optional<AuctionStage> currentStageOptional = getCurrentStage(now, auctionStages);
        if (currentStageOptional.isPresent()) {
            AuctionStage currentStage = currentStageOptional.get();
            int nextStageIndex = currentStage.getIndex() + 1;
            if (nextStageIndex == (auctionStages.size() + 1))
                return Optional.empty();
            return Optional.of(auctionStages.get(nextStageIndex));
        }
        return Optional.empty();
    }

    public List<AuctionStage> calculateAuctionStages(long startTime, long endTime) {
        List<AuctionStage> auctionStages = new LinkedList<>();
        long auctionDuration = (long) ((endTime - startTime) * 0.90); // 10% for special stage below

        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);

            long stateDuration = (long) (stage.getProportionTime() / proportionTotal * auctionDuration);

            long stageStartTime = startTime;
            long stageEndTime = stageStartTime + stateDuration;

            AuctionStage auctionStage = AuctionStage.from(stage);
            auctionStage.setIndex(i);
            auctionStage.setStartTime(stageStartTime);
            auctionStage.setEndTime(stageEndTime);

            auctionStages.add(auctionStage);

            startTime = stageEndTime;
        }

        Stage specialStage = new Stage(1000, "PUBLIC", 10F, 1000, "FINAL", 100.0F);

        AuctionStage specialAuctionStage = AuctionStage.from(specialStage);
        specialAuctionStage.setIndex(auctionStages.size());
        specialAuctionStage.setStartTime(startTime);
        specialAuctionStage.setEndTime(endTime);

        auctionStages.add(specialAuctionStage);
        auctionStages.sort(Comparator.comparingInt(Stage::getSort));
        return auctionStages;
    }
}
