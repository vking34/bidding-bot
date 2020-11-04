package com.chozoi.auction_bot_v2.auctions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstantBidRequest {
    private long auctionId;
    private long clientTime;
    private long price;
    private String type;
}