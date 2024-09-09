package io.playground.scraper.model.geolocation;

public record IPApiResponse(
        String query,
        String status,
        String message,
        String country,
        String countryCode,
        String region,
        String regionName,
        String city,
        String zip,
        Double lat,
        Double lon,
        String timezone,
        String isp,
        String org,
        String as
) {
}
