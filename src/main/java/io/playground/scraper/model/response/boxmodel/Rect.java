package io.playground.scraper.model.response.boxmodel;

public record Rect(
        int x,
        int y,
        int width,
        int height,
        int scrollLeft,
        int scrollTop,
        int clientLeft,
        int clientTop,
        int clientWidth,
        int clientHeight
) {
}
