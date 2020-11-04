package com.chozoi.auction_bot_v2.bots.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LateBot {
    private Long auctionId;
    private Integer randomBids;
    private Long delayForEachInstantBid;
}
