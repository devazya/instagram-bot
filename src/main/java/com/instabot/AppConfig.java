package com.instabot;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Central place for Spring-level configuration switches.
 *
 * @EnableAsync activates the @Async annotation used by
 * WebhookController.handleAsync(), so comment processing (and the outbound
 * Graph API call) runs on a background thread instead of blocking the
 * HTTP response Meta is waiting on.
 *
 * @EnableScheduling activates the @Scheduled annotation used by
 * CommentPollingService, which periodically checks for new comments via
 * the Graph API instead of relying on Meta webhooks (avoids the need to
 * publish the app / complete Business Verification).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {
}
