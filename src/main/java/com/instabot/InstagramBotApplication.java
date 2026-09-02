package com.instabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the Instagram Comment-to-DM automation bot.
 *
 * Run with:  mvn spring-boot:run
 * Or build a jar:  mvn clean package  ->  java -jar target/instagram-bot-1.0.0.jar
 */
@SpringBootApplication
@EnableConfigurationProperties(MetaConfig.class)
public class InstagramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstagramBotApplication.class, args);
    }

    /**
     * RestTemplate is what we use to make outbound HTTP calls to the
     * Meta Graph API (the "Private Reply" dispatch in MetaGraphApiService).
     * Declaring it as a @Bean lets Spring inject it wherever needed.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
