package com.chozoi.auction_bot_v2.bots.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("Bot")
public class Bot implements Serializable {

    @Id
    private Long auctionId;

    private Long priceTarget;
    private Boolean isStarted;
}
