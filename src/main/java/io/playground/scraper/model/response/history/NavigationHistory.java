package io.playground.scraper.model.response.history;

import java.util.List;

public record NavigationHistory(int currentIndex, List<NavigationHistoryEntry> entries) {
}
