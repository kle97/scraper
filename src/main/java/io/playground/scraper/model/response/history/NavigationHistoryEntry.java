package io.playground.scraper.model.response.history;

public record NavigationHistoryEntry(
        int id,
        String url,
        String userTypedURL,
        String title,
        String transitionType
) {
}
