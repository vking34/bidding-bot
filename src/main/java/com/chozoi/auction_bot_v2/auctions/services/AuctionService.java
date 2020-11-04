package com.chozoi.auction_bot_v2.auctions.services;


import com.chozoi.auction_bot_v2.auctions.dto.*;
import com.chozoi.auction_bot_v2.auctions.models.Auction;
import com.chozoi.auction_bot_v2.auctions.repositories.AuctionRepository;
import com.chozoi.auction_bot_v2.auctions.repositories.FlashBidResultRepository;
import com.chozoi.auction_bot_v2.users.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Service
@Slf4j
public class AuctionService {
    @Value("${auction.service.url}")
    private String AUCTION_SERVICE_URL;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private FlashBidResultRepository flashBidResultRepository;

    @Autowired
    private RestTemplate restTemplate;

    public List<Auction> getRunningFlashBids() {
        return auctionRepository.findFlashBids();
    }

    public Auction getAuction(Long auctionId) {
        return auctionRepository.getOne(auctionId);
    }

    public AuctionDto fetchAuction(Long auctionId) {
        String url = String.format("%s/%s", AUCTION_SERVICE_URL, auctionId);
        log.warn("url: {}", url);
        AuctionResponse response = restTemplate.getForObject(url, AuctionResponse.class);
        if (response == null)
            return null;
        return response.getAuction();
    }

    public void fetchAuctionResult(AuctionDto auction) {
        String url = String.format("%s/%s/result", AUCTION_SERVICE_URL, auction.getId());
        AuctionResultResponse response = restTemplate.getForObject(url, AuctionResultResponse.class);
        if (response != null)
            auction.setResult(response.getResult());
    }

    public AuctionResultDto fetchAuctionResult(Long auctionId) {
        try {
            String url = String.format("%s/%s/result", AUCTION_SERVICE_URL, String.valueOf(auctionId));
            AuctionResultResponse response = restTemplate.getForObject(url, AuctionResultResponse.class);
            if (response != null)
                return response.getResult();
            return null;
        } catch (RestClientException ignored) {
            return null;
        }
    }

    public BidResponse instantBid(long auctionId, long price, int userId) {
        String url = String.format("%s/%s/bids", AUCTION_SERVICE_URL, auctionId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Chozoi-User-Id", String.valueOf(userId));
        headers.set("X-Chozoi-Service-Id", "auction-bot");
        InstantBidRequest instantBidRequest = new InstantBidRequest(auctionId, System.currentTimeMillis(), price, "MANUAL");
        HttpEntity<InstantBidRequest> request = new HttpEntity<>(instantBidRequest, headers);

        return restTemplate.postForObject(url, request, BidResponse.class);
    }

    public BidResponse autoBid(long auctionId, long price, int userId) {
        String url = String.format("%s/%s/bids/auto", AUCTION_SERVICE_URL, auctionId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Chozoi-User-Id", String.valueOf(userId));
        headers.set("X-Chozoi-Service-Id", "auction-bot");
        AutoBidRequest autoBidRequest = new AutoBidRequest(price, System.currentTimeMillis());
        HttpEntity<AutoBidRequest> request = new HttpEntity<>(autoBidRequest, headers);

        return restTemplate.postForObject(url, request, BidResponse.class);
    }

    public User getCurrentWinner(Long auctionId, long phaseId){
        return flashBidResultRepository.findByIdAndPhaseId(auctionId, phaseId).getWinner();
    }

}
