package io.playground.scraper.model.response.window;

public record WindowBounds(
        Integer left,
        Integer top,
        Integer width,
        Integer height, 
        String windowState
) {
}
