package com.instabot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible ONLY for talking to Meta's Graph API.
 * Keeps HTTP/auth concerns separate from the webhook parsing logic
 * (see CommentProcessorService).
 */
@Service
public class MetaGraphApiService {

    private static final Logger log = LoggerFactory.getLogger(MetaGraphApiService.class);

    private final RestTemplate restTemplate;
    private final MetaConfig metaConfig;

    public MetaGraphApiService(RestTemplate restTemplate, MetaConfig metaConfig) {
        this.restTemplate = restTemplate;
        this.metaConfig = metaConfig;
    }

    /**
     * Sends a "Private Reply" DM to the user who left the comment.
     *
     * Graph API endpoint:
     *   POST https://graph.facebook.com/{version}/{page_id}/messages?access_token={token}
     *
     * Body:
     *   {
     *     "recipient": { "comment_id": "<commentId>" },
     *     "message":   { "text": "<replyText>" }
     *   }
     *
     * @param commentId the id extracted from the incoming webhook comment payload
     */
    public void sendPrivateReply(String commentId) {
        String url = String.format(
                "https://graph.facebook.com/%s/%s/messages?access_token=%s",
                metaConfig.getGraphApiVersion(),
                metaConfig.getPageId(),
                metaConfig.getPageAccessToken()
        );

        // --- Build the nested JSON body as a Map (Jackson serializes this automatically) ---
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("comment_id", commentId);

        Map<String, Object> message = new HashMap<>();
        message.put("text", metaConfig.getReplyMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("recipient", recipient);
        body.put("message", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("Private reply sent for comment_id={} | Meta response status={} body={}",
                    commentId, response.getStatusCode(), response.getBody());
        } catch (HttpClientErrorException ex) {
            // Meta returns 4xx with a descriptive JSON error body — log it fully so
            // permission/scope issues (common in Development Mode) are easy to diagnose.
            log.error("Meta Graph API rejected the private reply for comment_id={}. Status={} Body={}",
                    commentId, ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Unexpected error sending private reply for comment_id={}: {}", commentId, ex.getMessage(), ex);
        }
    }
}
