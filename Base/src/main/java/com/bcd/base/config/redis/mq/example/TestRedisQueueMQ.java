package com.bcd.base.config.redis.mq.example;

import com.bcd.base.config.redis.mq.queue.RedisQueueMQ;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class TestRedisQueueMQ extends RedisQueueMQ<String>{
    public TestRedisQueueMQ(RedisConnectionFactory redisConnectionFactory) {
        super("test",redisConnectionFactory,String.class);
        watch();
    }

    @Override
    public void onMessage(String data) {
        System.out.println(data);
    }
}
