package com.chozoi.auction_bot_v2.auctions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuctionResponse {
    @JsonProperty("auction")
    private AuctionDto auction;
}
