package com.chozoi.auction_bot_v2;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@SpringBootApplication
@EnableRedisRepositories
@EnableWebMvc
@EnableScheduling
public class AuctionBotV2Application {

    public static void main(String[] args) {
        SpringApplication.run(AuctionBotV2Application.class, args);
    }

    @Bean
    public ScheduledExecutorService executorService() {
        return new ScheduledThreadPoolExecutor(10);
    }

    @Bean
    public RestTemplate rest(RestTemplateBuilder builder) {
        return builder.build();
    }
}
