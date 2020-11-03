package com.chozoi.auction_bot_v2.bots.services;

import com.chozoi.auction_bot_v2.bots.models.Bot;
import com.chozoi.auction_bot_v2.bots.repositories.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class BotService {

    @Autowired
    private BotRepository botRepository;

    public Bot getBot(Long auctionId){
        Optional<Bot> bot = botRepository.findById(auctionId);
        return bot.orElse(null);
    }

    public void saveBot(Bot bot){
        botRepository.save(bot);
    }

    public void removeBot(Long auctionId){
        botRepository.deleteById(auctionId);
    }

    public void deleteAllBots(){
        botRepository.deleteAll();
    }
}
