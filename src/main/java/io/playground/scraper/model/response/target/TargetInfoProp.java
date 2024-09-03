package io.playground.scraper.model.response.target;

public record TargetInfoProp(
        String targetId,
        String type,
        String title,
        String url,
        Boolean attached,
        Boolean canAccessOpener,
        String browserContextId
) {
}
