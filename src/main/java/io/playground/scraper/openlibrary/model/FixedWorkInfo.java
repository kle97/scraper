package io.playground.scraper.openlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record FixedWorkInfo(
        String title,
        String description
) {
}
