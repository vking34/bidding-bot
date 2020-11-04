package com.chozoi.auction_bot_v2.bots.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NBidResultDto {
    private Integer randomPriceSteps;
    private Boolean lastStep = false;
}
