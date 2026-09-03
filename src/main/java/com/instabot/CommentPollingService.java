package com.instabot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls the Graph API on a fixed interval for new comments, instead of
 * relying on Meta webhooks.
 *
 * Why this exists: Instagram Messaging webhooks require the app to be
 * "Published", which in turn requires Business Verification (a legal
 * document / ID review process). For a personal-use bot on your own
 * account, that's disproportionate — so this polls your own recent media
 * for new comments using the same Standard Access Page token you already
 * have, no publishing required.
 *
 * Trade-off vs webhooks: replies go out on the next poll cycle
 * (meta.polling-interval-ms, default 60s) instead of instantly.
 *
 * De-duplication: processedCommentIds is an in-memory set, so it resets on
 * app restart. If you restart the app right after a real comment came in
 * but before its poll cycle ran, it could theoretically be processed again
 * on the next run — acceptable for personal-use volume, but worth knowing.
 */
@Service
public class CommentPollingService {

    private static final Logger log = LoggerFactory.getLogger(CommentPollingService.class);

    private final RestTemplate restTemplate;
    private final MetaConfig metaConfig;
    private final CommentProcessorService commentProcessorService;

    // Comment IDs we've already processed, so we never send a duplicate DM
    // for the same comment on a later poll cycle.
    private final Set<String> processedCommentIds = ConcurrentHashMap.newKeySet();

    public CommentPollingService(RestTemplate restTemplate,
                                  MetaConfig metaConfig,
                                  CommentProcessorService commentProcessorService) {
        this.restTemplate = restTemplate;
        this.metaConfig = metaConfig;
        this.commentProcessorService = commentProcessorService;
    }

    /**
     * Runs every meta.polling-interval-ms milliseconds (default 60000 = 60s).
     * fixedDelay means the wait is measured from the END of the previous run,
     * so slow Graph API responses never cause overlapping polls.
     */
    @Scheduled(fixedDelayString = "${meta.polling-interval-ms:60000}")
    public void pollForNewComments() {
        String igAccountId = metaConfig.getInstagramAccountId();
        String token = metaConfig.getPageAccessToken();

        if (igAccountId == null || igAccountId.isBlank() || igAccountId.startsWith("CHANGE_ME")) {
            log.warn("meta.instagram-account-id is not configured — skipping poll cycle. " +
                    "Set META_IG_ACCOUNT_ID in .env.");
            return;
        }

        // Note: { and } are not legal literal characters in a URI's query string,
        // so the "comments{id,text}" field-expansion syntax must be percent-encoded
        // as %7B / %7D, or java.net.URI.create() rejects it with
        // "Illegal character in query".
        String url = String.format(
                "https://graph.facebook.com/%s/%s/media?fields=id,comments%%7Bid,text%%7D&access_token=%s",
                metaConfig.getGraphApiVersion(), igAccountId, token
        );

        try {
            // Build a URI directly instead of passing a raw String to RestTemplate:
            // getForEntity(String, ...) treats { } in the URL as URI-template
            // placeholders (e.g. Spring would try to "expand" comments{id,text}),
            // which throws "Not enough variable values available to expand".
            // URI.create() takes the string as-is, no template parsing.
            URI uri = URI.create(url);

            ResponseEntity<InstagramMediaResponse> response =
                    restTemplate.getForEntity(uri, InstagramMediaResponse.class);

            InstagramMediaResponse body = response.getBody();
            if (body == null || body.getData() == null) {
                log.debug("Poll cycle: no media data returned.");
                return;
            }

            int newCommentsSeen = 0;

            for (InstagramMediaResponse.MediaItem media : body.getData()) {
                if (media.getComments() == null || media.getComments().getData() == null) {
                    continue;
                }

                List<CommentData> comments = media.getComments().getData();
                for (CommentData comment : comments) {
                    if (comment.getId() == null || comment.getText() == null) {
                        continue;
                    }

                    // Skip comments we've already handled on a previous poll cycle.
                    if (!processedCommentIds.add(comment.getId())) {
                        continue;
                    }

                    newCommentsSeen++;
                    commentProcessorService.processComment(comment.getId(), comment.getText());
                }
            }

            log.debug("Poll cycle complete. New comments seen: {}", newCommentsSeen);

        } catch (HttpClientErrorException ex) {
            log.error("Poll cycle failed \u2014 Meta rejected the media/comments request. Status={} Body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Poll cycle failed with an unexpected error: {}", ex.getMessage(), ex);
        }
    }
}
