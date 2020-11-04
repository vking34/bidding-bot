package com.chozoi.auction_bot_v2.auctions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoBidRequest {
    @JsonProperty("ceilingPrice")
    private long ceilingPrice;
    @JsonProperty("clientTime")
    private long clientTime;

    @Override
    public String toString(){
        return "ceilingPrice=" + this.ceilingPrice + ", clientTime=" + this.clientTime;
    }
}