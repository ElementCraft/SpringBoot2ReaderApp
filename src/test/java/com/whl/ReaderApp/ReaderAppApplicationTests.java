package com.whl.ReaderApp;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReaderAppApplicationTests {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Test
    public void contextLoads() {
        redisTemplate.opsForHash().values("Books").toIterable().forEach(null);

    }

}
