package io.playground.scraper.model.chromedownload;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DownloadLinks(List<DownloadPlatform> chrome, List<DownloadPlatform> chromedriver, 
                            @JsonProperty("chrome-headless-shell") List<DownloadPlatform> chromeHeadlessShell) {
}
