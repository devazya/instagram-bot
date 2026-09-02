package com.instabot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single webhook URL, two HTTP methods:
 *   GET  /webhook  -> Meta's one-time verification handshake
 *   POST /webhook  -> Meta's actual event delivery (comments etc.)
 *
 * Both are exposed at the same path because that's what Meta expects
 * when you register the "Callback URL" in the App Dashboard.
 */
@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final MetaConfig metaConfig;
    private final CommentProcessorService commentProcessorService;

    public WebhookController(MetaConfig metaConfig, CommentProcessorService commentProcessorService) {
        this.metaConfig = metaConfig;
        this.commentProcessorService = commentProcessorService;
    }

    /**
     * Health check endpoint to keep the tunnel alive.
     */
    @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("Bot is awake!");
    }

    /**
     * STEP 1 — Webhook verification (one-time, done when you click "Verify and Save"
     * in the Meta dashboard).
     *
     * Meta calls:
     *   GET /webhook?hub.mode=subscribe&hub.challenge=RANDOM_STRING&hub.verify_token=YOUR_SECRET
     *
     * We MUST:
     *   - Check hub.verify_token equals our configured secret
     *   - If it matches, return hub.challenge as plain text with 200 OK
     *   - If it does NOT match, return 403 Forbidden (so no one can spoof our webhook)
     */
    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken) {

        log.info("Webhook verification attempt: mode={}, tokenProvided={}", mode, verifyToken != null);

        boolean modeOk = "subscribe".equals(mode);
        boolean tokenOk = metaConfig.getVerifyToken() != null && metaConfig.getVerifyToken().equals(verifyToken);

        if (modeOk && tokenOk) {
            log.info("Webhook verified successfully. Echoing back hub.challenge.");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Webhook verification FAILED. mode={}, tokenMatched={}", mode, tokenOk);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * STEP 2 — Real-time event delivery.
     *
     * Meta calls this every time a subscribed event happens (e.g. a new comment).
     * Contract with Meta: respond 200 OK immediately (within a few seconds), or
     * Meta will treat it as a failure and retry — which can spam our server and
     * cause duplicate DMs if we're not careful.
     *
     * So: acknowledge FIRST, then process the payload asynchronously.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveEvent(@RequestBody WebhookPayload payload) {
        log.debug("Webhook POST received: object={}", payload != null ? payload.getObject() : "null");

        // Hand off to async processing so Meta gets its 200 OK immediately.
        handleAsync(payload);

        return ResponseEntity.ok().build();
    }

    /**
     * Runs on a separate thread (see AppConfig for @EnableAsync) so the
     * controller method above can return instantly.
     */
    @Async
    public void handleAsync(WebhookPayload payload) {
        try {
            commentProcessorService.process(payload);
        } catch (Exception ex) {
            // Never let an exception here surface anywhere near Meta's request/response cycle —
            // by this point we've already returned 200 OK, this is purely internal.
            log.error("Error while asynchronously processing webhook payload: {}", ex.getMessage(), ex);
        }
    }
}
