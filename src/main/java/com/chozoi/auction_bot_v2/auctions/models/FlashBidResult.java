package com.chozoi.auction_bot_v2.auctions.models;


import com.chozoi.auction_bot_v2.users.models.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_result_flash_bid", schema = "auctions")
@Data
@NoArgsConstructor
public class FlashBidResult {
    @Id
    private Long id;

//    @Column(name = "bidders_count")
//    private Integer bidderCount;
//
//    @Column(name = "bids_count")
//    private Integer bidCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = User.class)
    @JoinColumn(name = "winner_id", referencedColumnName = "id")
    private User winner;

    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "ceiling_price")
    private Long ceilingPrice;

    @Column(name = "phase_id")
    private Long phaseId;
}