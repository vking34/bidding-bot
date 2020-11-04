package com.chozoi.auction_bot_v2.bots;

import com.chozoi.auction_bot_v2.auctions.constants.BidState;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionDto;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionResultDto;
import com.chozoi.auction_bot_v2.auctions.services.AuctionService;
import com.chozoi.auction_bot_v2.bots.dto.LateBot;
import com.chozoi.auction_bot_v2.bots.dto.NBidResultDto;
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

import static com.chozoi.auction_bot_v2.bots.constants.Queue.FLASH_BID_AFTER_MIN_PRICE_BOT_QUEUE;
import static com.chozoi.auction_bot_v2.bots.constants.Queue.FLASH_BID_BOT_QUEUE;

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
        log.warn("Bot: {}", bot);
        Long auctionId = bot.getAuctionId();
        AuctionDto auction = auctionService.fetchAuction(auctionId);
        log.warn("Auction: {}", auction);
        if (!auction.getState().equals(BidState.BIDDING)) {
            botService.removeBot(auctionId);
            return;
        }

        long
                remainingTimeInCurrentStage,
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



    }

    @RqueueListener(value = FLASH_BID_AFTER_MIN_PRICE_BOT_QUEUE, numRetries = "3", deadLetterQueue = "failed-after-min-price-bot-queue", concurrency = "4-6")
    public void makeInstantBidAfterMinPrice(LateBot lateBot) {
        Long auctionId = lateBot.getAuctionId();
        try {
            if (lateBot.getRandomBids() > 0) {
                AuctionResultDto auctionResult = auctionService.fetchAuctionResult(auctionId);
                if (auctionResult == null) {
                    botService.removeBot(auctionId);
                }

                if (auctionResult.getTimeEnd() > System.currentTimeMillis()) {
                    User user = auctionService.getCurrentWinner(auctionId, auctionResult.getPhaseId());
                }
            }
        }
        catch (Exception ignored) {
            botService.removeBot(auctionId);
        }
    }

    public NBidResultDto makeFlashBid(long auctionId, long currentPrice, long priceStep, long highestAutoBid, long currentStagePriceThreshold, long priceTarget) {
        NBidResultDto nBidResultDto = new NBidResultDto();
        int randomPriceSteps = getRandomPriceSteps(20);
        if (randomPriceSteps == 1) {
            makeInstantBid(auctionId, currentPrice, priceStep);
        } else {
            long ceilingPrice = highestAutoBid > currentPrice ? highestAutoBid + randomPriceSteps * priceStep : currentPrice + randomPriceSteps * priceStep;
            if (ceilingPrice >= currentStagePriceThreshold) {
                // System.out.println(this.productId + " - over ceiling price");
                if (ceilingPrice >= priceTarget) {
                    nBidResultDto.setLastStep(true); // mark the next bid as the last bid
                    makeAutoBid(auctionId, priceTarget - priceStep);
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

    private int getRandomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound) + 1;
    }

    private int getRandomPriceSteps(int priceStepBound) {
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
