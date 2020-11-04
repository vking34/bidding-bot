package com.chozoi.auction_bot_v2.auctions.dto;

import lombok.Data;

@Data
public class BidResponse {
    private Bid bid;

    public BidResponse() {}

    public BidResponse(String uuid, long serverTime) {
        Bid bid = new Bid();
        bid.uuid = uuid;
        bid.serverTime = serverTime;

        this.bid = bid;
    }

    @Data
    public class Bid {

        String uuid;
        long serverTime;
    }
}
