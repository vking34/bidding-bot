package com.chozoi.auction_bot_v2.bots.services;

import com.chozoi.auction_bot_v2.bots.models.Bot;
import com.chozoi.auction_bot_v2.bots.models.FakeUser;
import com.chozoi.auction_bot_v2.bots.repositories.BotRepository;
import com.chozoi.auction_bot_v2.bots.repositories.FakeUserRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class BotService {

    @Autowired
    private BotRepository botRepository;

    @Autowired
    private FakeUserRepository fakeUserRepository;

    @Getter
    private List<Integer> fakeUserIds;

    @Getter
    private Integer fakeUserSize;

    @PostConstruct
    private void loadFakeUsers(){
        List<FakeUser> fakeUsers = fakeUserRepository.findAll();
        fakeUserIds = fakeUsers.stream().mapToInt(FakeUser::getId).boxed().collect(Collectors.toList());
        fakeUserSize = fakeUserIds.size();
        log.warn("Loaded fake users: {}", fakeUserSize);
    }

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
