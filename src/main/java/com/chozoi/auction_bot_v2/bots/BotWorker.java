package com.chozoi.auction_bot_v2.bots;

import com.chozoi.auction_bot_v2.auctions.constants.BidState;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionDto;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionResultDto;
import com.chozoi.auction_bot_v2.auctions.services.AuctionService;
import com.chozoi.auction_bot_v2.bots.dto.LateBot;
import com.chozoi.auction_bot_v2.bots.dto.NBidResultDto;
import com.chozoi.auction_bot_v2.bots.dto.PaddingInstantBid;
import com.chozoi.auction_bot_v2.bots.models.Bot;
import com.chozoi.auction_bot_v2.bots.services.BotService;
import com.chozoi.auction_bot_v2.strategies.dto.AuctionStage;
import com.chozoi.auction_bot_v2.strategies.services.StrategyService;
import com.chozoi.auction_bot_v2.users.models.User;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import com.github.sonus21.rqueue.core.RqueueMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.chozoi.auction_bot_v2.bots.constants.Queue.*;

@Component
@Slf4j
public class BotWorker {

    @Autowired
    private RqueueMessageSender rqueueMessageSender;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private BotService botService;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private ScheduledExecutorService executorService;

    @RqueueListener(value = FLASH_BID_BOT_QUEUE, numRetries = "3", deadLetterQueue = "failed-bot-queue", concurrency = "5-10")
    public void executeFlashBidBot(Bot bot) {
        try {
            log.warn("Bot: {}", bot);
            Long auctionId = bot.getAuctionId();
            AuctionDto auction = auctionService.fetchAuction(auctionId);
            log.warn("Auction: {}", auction);
            if (!auction.getState().equals(BidState.BIDDING)) {
                botService.removeBot(auctionId);
                return;
            }

            long
                    remainingTimeInCurrentStage = 0,
                    currentPrice = 0,
                    priceStep = 0,
                    currentStagePriceThreshold = 0,
                    highestAutoBid = 0,
                    targetPrice = bot.getPriceTarget(),
                    now = System.currentTimeMillis(),
                    startTime = auction.getTimeStart(),
                    endTime = auction.getTimeEnd();

            List<AuctionStage> auctionStages = strategyService.calculateAuctionStages(startTime, endTime);
            Optional<AuctionStage> currentStageOptional = strategyService.getCurrentStage(now, auctionStages);
            if (currentStageOptional.isPresent()) {
                AuctionStage currentStage = currentStageOptional.get();
                log.warn("current stage: {}", currentStage);
                //
                long stageEndTime = currentStage.getEndTime();
                remainingTimeInCurrentStage = stageEndTime - now;

                //
                auctionService.fetchAuctionResult(auction);
                currentPrice = auction.getResult().getCurrentPrice();
                highestAutoBid = auction.getResult().getPriceBidAutoHighest();
                currentStagePriceThreshold = (long) Math.ceil(targetPrice * currentStage.getPriceTarget() / 100.0);
                priceStep = auction.getPriceStep();
            } else {
                remainingTimeInCurrentStage = 0;
            }

            if (remainingTimeInCurrentStage <= 0 || currentPrice >= currentStagePriceThreshold) {
                Optional<AuctionStage> nextStageOptional = strategyService.getNextStage(now, auctionStages);
                if (nextStageOptional.isPresent()) {    // wait for the next stage
                    rqueueMessageSender.enqueueIn(FLASH_BID_BOT_QUEUE, bot, remainingTimeInCurrentStage);
                } else {  // the auction finished
                    long delayForNextPhase = endTime - System.currentTimeMillis();
                    if (delayForNextPhase < 0) {
                        botService.removeBot(bot.getAuctionId());
                    }

                    // wait for the next phase
                    rqueueMessageSender.enqueueIn(FLASH_BID_BOT_QUEUE, bot, delayForNextPhase + 7000);
                }
                return;
            }

            // n-price-step bid
            NBidResultDto nBidResult = makeFlashBid(auctionId, currentPrice, priceStep, highestAutoBid, currentStagePriceThreshold, bot.getPriceTarget());
            if (nBidResult.getLastStep()) {
                doLastStepInFlashBid(bot, auctionId, targetPrice, endTime, priceStep);
                return;
            }

            // schedule the next execution
            long nextPrice = highestAutoBid > currentPrice ? highestAutoBid + priceStep : currentPrice + priceStep;
            long priceDelta = currentStagePriceThreshold - nextPrice;
            long remainingPriceStepsInCurrentStage = priceDelta <= 0 ? 0 : (int) Math.ceil(priceDelta * 1.0 / priceStep);
            int priceStepsDiff = highestAutoBid > currentPrice ? (int) ((highestAutoBid - currentPrice) / priceStep) + 1 : nBidResult.getRandomPriceSteps();
            long delayForNextExecution;

            if (remainingPriceStepsInCurrentStage > 0) {
                if (priceStepsDiff >= remainingPriceStepsInCurrentStage)    // wait for the next stage
                    delayForNextExecution = remainingTimeInCurrentStage;
                else    // wait for running out of the highest auto bid
                    delayForNextExecution = (long) (remainingTimeInCurrentStage * (float) priceStepsDiff / remainingPriceStepsInCurrentStage);
            } else
                delayForNextExecution = this.getRandomDelay(500, 1500);

            rqueueMessageSender.enqueueIn(FLASH_BID_BOT_QUEUE, bot, delayForNextExecution);

            // padding manual bids
            padInstantBidsInFlashBid(delayForNextExecution, auctionId, priceStep, targetPrice);
        } catch (Exception e) {
            botService.removeBot(bot.getAuctionId());
        }
    }

    private void doLastStepInFlashBid(Bot bot, long auctionId, long targetPrice, long endTime, long priceStep) {
        executorService.schedule(
                () -> {
                    makeAutoBid(auctionId, targetPrice);
                },
                getRandomDelay(200, 1000),
                TimeUnit.MILLISECONDS);

        // schedule for the next phase
        long current = System.currentTimeMillis();
        long delayForNextPhase = endTime - current + 6500;
        rqueueMessageSender.enqueueIn(FLASH_BID_BOT_QUEUE, bot, delayForNextPhase);

        // after min price
        long delayForEachInstantBid = 5000;
        int priceStepRange = (int) ((bot.getMaxPrice() - targetPrice) / priceStep);
        int randomBids = getRandomInt(priceStepRange);
        LateBot lateBot = new LateBot(auctionId, randomBids, delayForEachInstantBid);
        rqueueMessageSender.enqueueIn(FLASH_BID_AFTER_MIN_PRICE_BOT_QUEUE, lateBot, delayForEachInstantBid);
    }

    @RqueueListener(value = FLASH_BID_AFTER_MIN_PRICE_BOT_QUEUE, numRetries = "3", deadLetterQueue = "failed-after-min-price-bot-queue", concurrency = "2-4")
    public void makeInstantBidAfterMinPrice(LateBot lateBot) {
        Long auctionId = lateBot.getAuctionId();
        int randomBids = lateBot.getRandomBids();
        try {
            if (randomBids > 0) {
                AuctionResultDto auctionResult = auctionService.fetchAuctionResult(auctionId);
                if (auctionResult == null) {
                    botService.removeBot(auctionId);
                }
                else {
                    long remainingTime = auctionResult.getTimeEnd() - System.currentTimeMillis();
                    if (remainingTime > 6500L) {
                        User winner = auctionService.getCurrentWinner(auctionId, auctionResult.getPhaseId());
                        if (winner.getRole().equals("BOT")){
                            makeAutoBid(auctionId, auctionResult.getCurrentPrice());
                            lateBot.setRandomBids(--randomBids);
                            rqueueMessageSender.enqueueIn(FLASH_BID_AFTER_MIN_PRICE_BOT_QUEUE, lateBot, lateBot.getDelayForEachInstantBid());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            botService.removeBot(auctionId);
        }
    }

    @RqueueListener(value = FLASH_BID_INSTANT_BID_PADDING_QUEUE, numRetries = "3", deadLetterQueue = "failed-instant-bid-padding-queue", concurrency = "4-6")
    private void padInstantBid(PaddingInstantBid instantBid){
        Long auctionId = instantBid.getAuctionId();
        AuctionResultDto auctionResult = auctionService.fetchAuctionResult(auctionId);
        if (auctionResult != null){
            Long currentPrice = auctionResult.getCurrentPrice();
            Long priceStep = instantBid.getPriceStep();
            if (currentPrice + priceStep <= instantBid.getTargetPrice()) {
                makeInstantBid(auctionId, currentPrice, priceStep);
            }
        }
    }

    private void padInstantBidsInFlashBid(long delayForNextExecution, Long auctionId, Long priceStep, Long targetPrice) {
        if (delayForNextExecution > 4000) {
            int randomManualBids = 2;
            if (delayForNextExecution > 8000) {
                randomManualBids = delayForNextExecution < 15000 ? ThreadLocalRandom.current().nextInt(4) + 1 : ThreadLocalRandom.current().nextInt(6) + 1;
            }

            for (int manualBidIndex = 0; manualBidIndex < randomManualBids; manualBidIndex++) {
                long delayForNextManualBid = (manualBidIndex * delayForNextExecution / randomManualBids) + this.getRandomDelay(800, 1500);
                rqueueMessageSender.enqueueIn(FLASH_BID_INSTANT_BID_PADDING_QUEUE, new PaddingInstantBid(auctionId, priceStep, targetPrice), delayForNextManualBid);
            }
        }
    }

    public NBidResultDto makeFlashBid(long auctionId, long currentPrice, long priceStep, long highestAutoBid, long currentStagePriceThreshold, long targetPrice) {
        NBidResultDto nBidResultDto = new NBidResultDto();
        int randomPriceSteps = getRandomPriceSteps(priceStep, targetPrice);
        if (randomPriceSteps == 1) {
            makeInstantBid(auctionId, currentPrice, priceStep);
        } else {
            long ceilingPrice = highestAutoBid > currentPrice ? highestAutoBid + randomPriceSteps * priceStep : currentPrice + randomPriceSteps * priceStep;
            if (ceilingPrice >= currentStagePriceThreshold) {
                // System.out.println(this.productId + " - over ceiling price");
                if (ceilingPrice >= targetPrice) {
                    nBidResultDto.setLastStep(true); // mark the next bid as the last bid
                    makeAutoBid(auctionId, targetPrice - priceStep);
                } else {
                    makeAutoBid(auctionId, currentStagePriceThreshold);
                }
                randomPriceSteps = highestAutoBid > currentPrice ? (int) ((currentStagePriceThreshold - highestAutoBid) / priceStep) : (int) ((currentStagePriceThreshold - currentPrice) / priceStep);

            } else {
                makeAutoBid(auctionId, ceilingPrice);
            }
        }

        nBidResultDto.setRandomPriceSteps(randomPriceSteps);
        return nBidResultDto;
    }


    private int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound) + 1;
    }

    private int getRandomPriceSteps(int priceStepBound) {
        return ThreadLocalRandom.current().nextInt(priceStepBound) + 1;
    }

    private int getRandomPriceSteps(Long priceStep, Long targetPrice) {
        int priceStepBound = 50;
        if (targetPrice <= 50000L)
            priceStepBound = 15;
        else if (priceStep >= 50000L)
            priceStepBound = 10;

        return ThreadLocalRandom.current().nextInt(priceStepBound) + 1;
    }

    private void makeAutoBid(long auctionId, long price) {
        int userId;
        userId = getRandomUser();
        try {
            auctionService.autoBid(auctionId, price, userId);
        } catch (Exception ignored) {
        }
    }

    private void makeInstantBid(long auctionId, long currentPrice, long priceStep) {
        int userId;
        userId = getRandomUser();
        try {
            auctionService.instantBid(auctionId, currentPrice + priceStep, userId);
        } catch (Exception ignored) {
        }
    }

    private int getRandomUser() {
        int userId;
        try {
            int index = ThreadLocalRandom.current().nextInt(botService.getFakeUserSize()) + 1;
            userId = botService.getFakeUserIds().get(index);
        } catch (Exception ignored) {
            userId = 1185;
        }

        return userId;
    }

    private int getRandomDelay(int startTime, int endTime) {
        return endTime > startTime ? ThreadLocalRandom.current().nextInt(endTime - startTime) + startTime : ThreadLocalRandom.current().nextInt(endTime) + 1;
    }
}
