package io.playground.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrowserInfo(
        @JsonProperty("Browser")
        String browser,
        
        @JsonProperty("Protocol-Version")
        String protocolVersion,

        @JsonProperty("User-Agent")
        String userAgent,

        @JsonProperty("V8-Version")
        String v8Version,
        
        @JsonProperty("WebKit-Version")
        String webkitVersion,
        
        String webSocketDebuggerUrl
) {}
