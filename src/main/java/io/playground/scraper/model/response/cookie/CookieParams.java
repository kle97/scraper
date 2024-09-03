package io.playground.scraper.model.response.cookie;

public record CookieParams(
        String name, 
        String value,
        String url,
        String domain,
        String path,
        Boolean secure,
        Boolean httpOnly,
        String sameSite,
        String expires,
        String priority,
        Boolean sameParty,
        String sourceScheme,
        Integer sourcePort,
        String partitionKey
) {
}
