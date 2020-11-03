package com.chozoi.auction_bot_v2.auctions.dto;

import lombok.Data;

@Data
public class AuctionResultDto {
    private Long id;

    private Integer bidsCount;

    private Integer biddersCount;

    private Integer winnerId;

    private String state;

    private Long timeStart;

    private Long timeEnd;

    private Long phaseId;

    private Long currentPrice;

    private Long ceilingPrice;

    private Long priceBidAutoHighest;
}
