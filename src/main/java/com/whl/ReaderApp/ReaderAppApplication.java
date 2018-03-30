package com.whl.ReaderApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRedisRepositories
public class ReaderAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReaderAppApplication.class, args);
    }
}
