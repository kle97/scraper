package io.playground.scraper.model;

public record DownloadChannel(String channel, String version, String revision, DownloadLinks downloads) {
}
