package io.playground.scraper.model.response.frame;

import java.util.List;

public record Frame(
        String id,
        String parentId,
        String loaderId,
        String name,
        String url,
        String domainAndRegistry,
        String securityOrigin,
        String mimeType,
        Object adFrameStatus,
        String secureContextType,
        String crossOriginIsolatedContextType,
        List<String> gatedAPIFeatures
) {
}
