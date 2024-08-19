package io.playground.scraper.model.chromedevtools;

public record PageInfo(
        String description,

        String devtoolsFrontendUrl,

        String id,

        String title,

        String type,

        String url,

        String webSocketDebuggerUrl
) {}
