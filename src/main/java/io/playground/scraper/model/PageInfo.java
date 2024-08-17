package io.playground.scraper.model;

public record PageInfo(
        String description,

        String devtoolsFrontendUrl,

        String id,

        String title,

        String type,

        String url,

        String webSocketDebuggerUrl
) {}
