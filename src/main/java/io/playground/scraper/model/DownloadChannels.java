package io.playground.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DownloadChannels(@JsonProperty("Stable") DownloadChannel stableChannel,
                               @JsonProperty("Beta") DownloadChannel betaChannel,
                               @JsonProperty("Dev") DownloadChannel devChannel,
                               @JsonProperty("Canary") DownloadChannel canaryChannel) {
}
