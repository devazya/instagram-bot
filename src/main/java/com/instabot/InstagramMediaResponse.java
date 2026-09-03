package com.instabot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the JSON shape returned by:
 *   GET /{ig-account-id}/media?fields=id,comments{id,text}&access_token=...
 *
 * {
 *   "data": [
 *     {
 *       "id": "media123",
 *       "comments": {
 *         "data": [
 *           { "id": "comment456", "text": "LINK please" }
 *         ]
 *       }
 *     }
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstagramMediaResponse {

    private List<MediaItem> data;

    public List<MediaItem> getData() {
        return data;
    }

    public void setData(List<MediaItem> data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MediaItem {
        private String id;
        private CommentsWrapper comments;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public CommentsWrapper getComments() {
            return comments;
        }

        public void setComments(CommentsWrapper comments) {
            this.comments = comments;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommentsWrapper {
        private List<CommentData> data;

        public List<CommentData> getData() {
            return data;
        }

        public void setData(List<CommentData> data) {
            this.data = data;
        }
    }
}
