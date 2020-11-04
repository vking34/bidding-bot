package com.chozoi.auction_bot_v2.bots;

import com.chozoi.auction_bot_v2.auctions.models.Auction;
import com.chozoi.auction_bot_v2.auctions.services.AuctionService;
import com.chozoi.auction_bot_v2.bots.models.Bot;
import com.chozoi.auction_bot_v2.bots.services.BotService;
import com.github.sonus21.rqueue.core.RqueueMessageSender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.chozoi.auction_bot_v2.bots.constants.Queue.FLASH_BID_BOT_QUEUE;

@Component
@Slf4j
public class BotScheduler {

    @Getter
    @Value("${auction.max-price-steps}")
    private Integer maxPriceSteps;

    @Autowired
    private ScheduledExecutorService executorService;

    @Autowired
    private RqueueMessageSender rqueueMessageSender;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private BotService botService;


    @PostConstruct
    private void loadAuctions() throws InterruptedException {
        executorService.schedule(() -> {    // wait for registering queue
            // clean up
            botService.deleteAllBots();
            rqueueMessageSender.deleteAllMessages(FLASH_BID_BOT_QUEUE);

            // reload auctions
            loadNormalBids();
            loadFlashBids();
        }, 3000, TimeUnit.MILLISECONDS);
    }

    private void loadNormalBids() {

    }

    @Scheduled(fixedDelay = 7000, initialDelay = 7000)
    private void loadFlashBids() {
        log.warn("Loading flash bids ...");
        List<Auction> flashBids = auctionService.getRunningFlashBids();
        log.warn("Flash bids: {}", flashBids);
        flashBids.forEach(auction -> {
            Bot bot = botService.getBot(auction.getId());
            if (bot.getAuctionId() == null) {
                bot = new Bot(
                        auction.getId(),
                        auction.getExpectedPrice(),
                        auction.getExpectedMaxPrice()
                        );
                botService.saveBot(bot);
                createFlashBidBot(bot);
            }
        });
    }

    private void createFlashBidBot(Bot bot) {
        rqueueMessageSender.enqueue(FLASH_BID_BOT_QUEUE, bot);
    }
}
