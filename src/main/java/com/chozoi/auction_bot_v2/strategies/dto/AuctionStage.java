package com.chozoi.auction_bot_v2.strategies.dto;

import com.chozoi.auction_bot_v2.strategies.models.Stage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AuctionStage extends Stage {

    private int index;

    private long startTime;

    private long endTime;

    private AuctionStage(Stage stage) {
        super(stage);
    }

    public static AuctionStage from(Stage stage) {
        return new AuctionStage(stage);
    }
}