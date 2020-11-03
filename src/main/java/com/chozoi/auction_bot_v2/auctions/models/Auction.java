package com.chozoi.auction_bot_v2.auctions.models;

import com.chozoi.auction_bot_v2.auctions.constants.PostgreSQLEnumType;
import com.chozoi.auction_bot_v2.auctions.constants.AuctionState;
import com.chozoi.auction_bot_v2.auctions.constants.AuctionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "auction", schema = "auctions")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
public class Auction {
    @Id
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(nullable = false, columnDefinition = "auction_state", name = "state")
    private AuctionState state;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(nullable = false, columnDefinition = "auction_state", name = "type")
    private AuctionType type;

    @Column(name = "time_start")
    private LocalDateTime timeStart;

    @Column(name = "time_end")
    private LocalDateTime timeEnd;

    @Column(name = "price_step")
    private long priceStep;

    @Column(name = "start_price")
    private long startPrice;

    @Column(name = "expected_price")
    private Long expectedPrice;

    @JsonProperty("expected_max_price")
    private Long expectedMaxPrice;

    @Column(name = "buy_now_price")
    private Long buyNowPrice;

    @Column(name = "original_price")
    private long originalPrice;

    @Column(name = "time_duration")
    private Integer timeDuration;

    @Column(name = "refuse_payment")
    private Boolean refusePayment;

    @Column(name = "phase_id")
    private Long phaseId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
