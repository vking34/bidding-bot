package com.chozoi.auction_bot_v2.auctions.repositories;

import com.chozoi.auction_bot_v2.auctions.models.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("SELECT a FROM Auction AS a WHERE a.type = 'FLASH_BID' AND a.state = 'BIDING' AND a.expectedPrice > 0 AND a.expectedMaxPrice >= a.expectedPrice")
    List<Auction> findFlashBids();
}
