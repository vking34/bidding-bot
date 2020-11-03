package com.chozoi.auction_bot_v2.auctions.services;


import com.chozoi.auction_bot_v2.auctions.dto.AuctionResponse;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionDto;
import com.chozoi.auction_bot_v2.auctions.dto.AuctionResultResponse;
import com.chozoi.auction_bot_v2.auctions.models.Auction;
import com.chozoi.auction_bot_v2.auctions.repositories.AuctionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private RestTemplate restTemplate;

    public List<Auction> getRunningFlashBids(){
        return auctionRepository.findFlashBids();
    }

    public Auction getAuction(Long auctionId){
        return auctionRepository.getOne(auctionId);
    }

    public AuctionDto fetchAuction(Long auctionId){
        String url = String.format("%s/%s", AUCTION_SERVICE_URL, auctionId);
        AuctionResponse response = restTemplate.getForObject(url, AuctionResponse.class);
        if (response == null)
            return null;
        return response.getAuction();
    }

    public void fetchAuctionResult(AuctionDto auction){
        String url = String.format("%s/%s/result", AUCTION_SERVICE_URL, auction.getId());
        AuctionResultResponse response = restTemplate.getForObject(url, AuctionResultResponse.class);
        if (response!= null)
            auction.setResult(response.getResult());
    }

}
