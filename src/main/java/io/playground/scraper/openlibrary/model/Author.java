package io.playground.scraper.openlibrary.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.playground.scraper.util.JacksonUtil;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Author(String name,
                     String title,
                     String birthDate,
                     String deathDate,
                     String date,
                     String key,
                     @JsonProperty("bio") JsonNode bioNode,
                     List<String> alternateNames,
                     List<Link> links,
                     List<String> photos) {
    public String bio() {
        if (bioNode != null) {
            if (bioNode.isTextual()) {
                return bioNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
            } else if (bioNode.isObject()) {
                return JacksonUtil.readValue(bioNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
            }
        }
        return null;
    }

    public String photo() {
        if (photos != null && !photos.isEmpty()) {
            return photos.getFirst();
        }
        return null;
    }

    public int olKey() {
        return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("A")));
    }
}
