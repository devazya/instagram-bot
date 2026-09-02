package com.instabot;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Central place for Spring-level configuration switches.
 *
 * @EnableAsync activates the @Async annotation used by
 * WebhookController.handleAsync(), so comment processing (and the outbound
 * Graph API call) runs on a background thread instead of blocking the
 * HTTP response Meta is waiting on.
 */
@Configuration
@EnableAsync
public class AppConfig {
}
