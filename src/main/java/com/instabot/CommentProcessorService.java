package com.instabot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contains the business logic that decides WHAT to do with an incoming comment.
 * Pure logic, no HTTP concerns — easy to unit test in isolation.
 */
@Service
public class CommentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(CommentProcessorService.class);

    private final MetaGraphApiService metaGraphApiService;
    private final MetaConfig metaConfig;

    public CommentProcessorService(MetaGraphApiService metaGraphApiService, MetaConfig metaConfig) {
        this.metaGraphApiService = metaGraphApiService;
        this.metaConfig = metaConfig;
    }

    /**
     * Walks the full webhook payload, and for every comment change whose text
     * matches one of our trigger keywords, fires a private reply DM.
     *
     * This is called asynchronously from the controller AFTER the 200 OK has
     * already been returned to Meta, so slow Graph API calls never risk a
     * webhook delivery timeout/retry storm.
     */
    public void process(WebhookPayload payload) {
        if (payload == null || payload.getEntry() == null) {
            log.warn("Received empty/malformed webhook payload — nothing to process.");
            return;
        }

        for (WebhookPayload.Entry entry : payload.getEntry()) {
            if (entry.getChanges() == null) continue;

            for (WebhookPayload.Change change : entry.getChanges()) {
                CommentData comment = change.getValue();
                if (comment == null || comment.getText() == null || comment.getId() == null) {
                    log.debug("Skipping change with missing comment text/id.");
                    continue;
                }

                if (matchesTriggerKeyword(comment.getText())) {
                    log.info("Trigger keyword matched in comment_id={} text=\"{}\" — dispatching private reply.",
                            comment.getId(), comment.getText());
                    metaGraphApiService.sendPrivateReply(comment.getId());
                } else {
                    log.debug("Comment_id={} did not match any trigger keyword. text=\"{}\"",
                            comment.getId(), comment.getText());
                }
            }
        }
    }

    /**
     * Checks a single comment (id + text) against the configured trigger keywords
     * and dispatches a private reply if it matches. Shared entry point used by
     * both the webhook path (process(WebhookPayload)) and CommentPollingService,
     * so the matching/dispatch logic lives in exactly one place.
     *
     * @return true if the comment matched and a reply was dispatched
     */
    public boolean processComment(String commentId, String commentText) {
        if (commentId == null || commentText == null) {
            return false;
        }

        if (matchesTriggerKeyword(commentText)) {
            log.info("Trigger keyword matched in comment_id={} text=\"{}\" — dispatching private reply.",
                    commentId, commentText);
            metaGraphApiService.sendPrivateReply(commentId);
            return true;
        }

        log.debug("Comment_id={} did not match any trigger keyword. text=\"{}\"", commentId, commentText);
        return false;
    }

    /**
     * Case-insensitive check: does the comment text contain ANY of our configured
     * trigger keywords (e.g. "LINK", "SETUP")?
     */
    private boolean matchesTriggerKeyword(String commentText) {
        List<String> keywords = metaConfig.getTriggerKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }

        String normalizedText = commentText.trim().toLowerCase();

        for (String keyword : keywords) {
            if (keyword != null && normalizedText.contains(keyword.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
