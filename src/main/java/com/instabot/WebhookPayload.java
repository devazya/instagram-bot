package com.instabot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mirrors the JSON structure Meta sends to the webhook POST endpoint.
 *
 * Real payload shape (simplified):
 * {
 *   "object": "instagram",
 *   "entry": [
 *     {
 *       "id": "...",
 *       "time": 169...,
 *       "changes": [
 *         {
 *           "field": "comments",
 *           "value": {
 *             "id": "17xxxx_comment_id",
 *             "text": "LINK please",
 *             "from": { "id": "...", "username": "..." },
 *             "media": { "id": "..." }
 *           }
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) means we don't need to model
 * every single field Meta might send — only the ones we actually use.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {

    private String object;

    private List<Entry> entry;

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<Entry> getEntry() {
        return entry;
    }

    public void setEntry(List<Entry> entry) {
        this.entry = entry;
    }

    // ---- Nested: entry[] ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private List<Change> changes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<Change> getChanges() {
            return changes;
        }

        public void setChanges(List<Change> changes) {
            this.changes = changes;
        }
    }

    // ---- Nested: entry[].changes[] ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private String field;
        private CommentData value;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public CommentData getValue() {
            return value;
        }

        public void setValue(CommentData value) {
            this.value = value;
        }
    }
}
