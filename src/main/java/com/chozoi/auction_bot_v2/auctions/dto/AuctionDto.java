package com.chozoi.auction_bot_v2.auctions.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
public class AuctionDto {
    private Long id;

    private String state;
    private String type;

    private LocalDateTime timeStart;

    private LocalDateTime timeEnd;

    private Long priceStep;

    private Long startPrice;

    private Long buyNowPrice;

    private Long originalPrice;

    private AuctionResultDto result;

    public long getTimeEnd() {
        return timeEnd.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public long getTimeStart() {
        return timeStart.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
