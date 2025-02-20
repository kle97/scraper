package io.playground.scraper.openlibrary.model;

public record Redirect(String location, String key) {

    public int olKey() {
        return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.length() - 2));
    }

    public int olKeyTarget() {
        return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.length() - 2));
    }

    public boolean isAuthor() {
        return location != null && location.trim().endsWith("A");
    }

    public boolean isWork() {
        return location != null && location.trim().endsWith("W");
    }

    public boolean isEdition() {
        return location != null && location.trim().endsWith("M");
    }
}
