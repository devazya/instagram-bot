package com.instabot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catches any exception that bubbles up from a REST controller and converts it
 * into a clean JSON-free error response instead of leaking a stack trace.
 *
 * IMPORTANT: This does NOT catch exceptions thrown inside handleAsync()
 * (that runs on a different thread, after the HTTP response is already sent) —
 * those are caught locally in WebhookController itself.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAnyException(Exception ex) {
        log.error("Unhandled exception in controller layer: {}", ex.getMessage(), ex);
        // We still return 200-ish behavior philosophy is not applied here on purpose:
        // this handler is a safety net for genuinely broken requests (e.g. bad JSON
        // on a route we don't expect), not for the webhook happy-path.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal error — check server logs.");
    }
}
