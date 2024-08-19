package io.playground.scraper.model.chromedownload;

public record DownloadChannel(String channel, String version, String revision, DownloadLinks downloads) {
}
