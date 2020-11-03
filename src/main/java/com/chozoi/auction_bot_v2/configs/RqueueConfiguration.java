package com.chozoi.auction_bot_v2.configs;

import com.github.sonus21.rqueue.config.SimpleRqueueListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RqueueConfiguration {

    @Bean
    public SimpleRqueueListenerContainerFactory simpleRqueueListenerContainerFactory() {
        SimpleRqueueListenerContainerFactory factory = new SimpleRqueueListenerContainerFactory();

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(System.getenv("REDIS_HOST"), Integer.parseInt(System.getenv("REDIS_PORT")));
        redisConfig.setDatabase(Integer.parseInt(System.getenv("REDIS_JOB_DB")));
        LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(redisConfig);
        redisConnectionFactory.afterPropertiesSet();
        factory.setRedisConnectionFactory(redisConnectionFactory);

        return factory;
    }
}
