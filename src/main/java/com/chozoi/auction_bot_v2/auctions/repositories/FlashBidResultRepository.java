package com.chozoi.auction_bot_v2.auctions.repositories;

import com.chozoi.auction_bot_v2.auctions.models.FlashBidResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashBidResultRepository extends JpaRepository<FlashBidResult, Long> {

//    @Query("SELECT new com.chozoi.auctionbot.users.models.User(u.id, u.role, u.state) FROM FlashBidResult AS a JOIN accounts.user AS u ON a.id = :auctionId AND a.phaseId = :phaseId AND a.winnerId = u.id")
//    public User findWinner(Long auctionId, Long phaseId);

    public FlashBidResult findByIdAndPhaseId(Long auctionId, Long phaseId);
}