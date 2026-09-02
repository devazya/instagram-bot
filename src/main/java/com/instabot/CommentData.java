package com.instabot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents entry[].changes[].value — the actual comment payload.
 * This is the piece we care about: the comment_id and the text typed by the user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentData {

    /** The unique comment ID. This is what we pass to the Private Reply API. */
    private String id;

    /** The raw text the user typed in their comment. */
    private String text;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
