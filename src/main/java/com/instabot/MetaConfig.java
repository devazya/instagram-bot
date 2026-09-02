package com.instabot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed binding of the "meta.*" properties from application.properties.
 * Spring Boot automatically populates this from the config file / env vars,
 * so we never scatter @Value("${...}") annotations across the codebase.
 */
@ConfigurationProperties(prefix = "meta")
public class MetaConfig {

    /** The secret string typed into the Meta Webhooks dashboard "Verify Token" field. */
    private String verifyToken;

    /** Long-lived Page Access Token used to authenticate outbound Graph API calls. */
    private String pageAccessToken;

    /** The Facebook Page ID linked to the Instagram account. */
    private String pageId;

    /** Graph API version, e.g. "v19.0". */
    private String graphApiVersion;

    /** Comma-separated list of keywords (case-insensitive) that trigger a DM. */
    private List<String> triggerKeywords;

    /** The message text sent back to the user via Private Reply. */
    private String replyMessage;

    // --- Getters and setters (required for Spring's property binding) ---

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getPageAccessToken() {
        return pageAccessToken;
    }

    public void setPageAccessToken(String pageAccessToken) {
        this.pageAccessToken = pageAccessToken;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public void setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }

    public List<String> getTriggerKeywords() {
        return triggerKeywords;
    }

    public void setTriggerKeywords(List<String> triggerKeywords) {
        this.triggerKeywords = triggerKeywords;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public void setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
    }
}
