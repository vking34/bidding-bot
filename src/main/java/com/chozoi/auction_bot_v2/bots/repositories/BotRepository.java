package com.chozoi.auction_bot_v2.bots.repositories;

import com.chozoi.auction_bot_v2.bots.models.Bot;
import org.springframework.data.repository.CrudRepository;

public interface BotRepository extends CrudRepository<Bot, Long> {
}
