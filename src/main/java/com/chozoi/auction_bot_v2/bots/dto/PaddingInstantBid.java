package com.chozoi.auction_bot_v2.bots.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaddingInstantBid {
    private Long auctionId;
    private Long priceStep;
    private Long targetPrice;
}
