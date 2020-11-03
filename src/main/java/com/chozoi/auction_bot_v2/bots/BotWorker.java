package com.chozoi.auction_bot_v2.bots;

import com.chozoi.auction_bot_v2.auctions.constants.BidState;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionDto;
import com.chozoi.auction_bot_v2.auctions.services.AuctionService;
import com.chozoi.auction_bot_v2.bots.models.Bot;
import com.chozoi.auction_bot_v2.bots.services.BotService;
import com.chozoi.auction_bot_v2.strategies.dto.AuctionStage;
import com.chozoi.auction_bot_v2.strategies.services.StrategyService;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.chozoi.auction_bot_v2.bots.constants.Queue.FLASH_BID_BOT_QUEUE;

@Component
@Slf4j
public class BotWorker {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private BotService botService;

    @Autowired
    private StrategyService strategyService;

    @RqueueListener(value = FLASH_BID_BOT_QUEUE, numRetries = "3", deadLetterQueue = "failed-bot-queue", concurrency = "5-10")
    public void executeFlashBidBot(Bot bot) {
        log.warn("Bot: {}", bot);
        Long auctionId = bot.getAuctionId();
        AuctionDto auction = auctionService.fetchAuction(auctionId);
        if (auction.getState().equals(BidState.BIDDING)) {
            botService.removeBot(auctionId);
            return;
        }

        long
                remainingTimeInCurrentStage = 0,
                remainingPriceStepsInCurrentStage = 0,
                currentPrice = 0,
                priceStep = 0,
                currentStagePriceThreshold = 0,
                highestAutoBid = 0,
                now = System.currentTimeMillis(),
                startTime = auction.getTimeStart(),
                endTime = auction.getTimeEnd();

        Optional<AuctionStage> auctionStageOptional = strategyService.getCurrentStage(startTime, endTime, now);
        if (auctionStageOptional.isPresent()) {
            AuctionStage auctionStage = auctionStageOptional.get();

            //
            long stageEndTime = auctionStage.getEndTime();
            remainingTimeInCurrentStage = stageEndTime - now;

            //
            auctionService.fetchAuctionResult(auction);
            currentPrice = auction.getResult().getCurrentPrice();
            highestAutoBid = auction.getResult().getPriceBidAutoHighest();
            currentStagePriceThreshold = (long) Math.ceil(bot.getPriceTarget() * auctionStage.getPriceTarget() / 100.0);
            priceStep = auction.getPriceStep();

        } else {
            remainingTimeInCurrentStage = 0;
            remainingPriceStepsInCurrentStage = 0;
        }




    }
}
