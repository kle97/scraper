package io.playground.scraper.openlibrary.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.playground.scraper.util.JacksonUtil;

import java.util.List;
import java.util.stream.Collectors;

public record Work(String title,
                   String key,
                   @JsonProperty("authors") JsonNode authorNodes,
                   List<String> subjects,
//                       List<String> subjectPlaces,
//                       List<String> subjectPeople,
//                       List<String> subjectTimes,
                   List<String> covers) {
    public List<String> authors() {
        if (authorNodes != null) {
            if (authorNodes.isTextual()) {
                return List.of(authorNodes.asText());
            } else {
                try {
                    List<AuthorNode> nodes = JacksonUtil.readValue(authorNodes, new TypeReference<>() {});
                    return nodes.stream().map(node -> node.author().key()).collect(Collectors.toList());
                } catch (Exception ignored) {
                }
            }
        }
        return List.of();
    }

    public String cover() {
        if (covers != null && !covers.isEmpty()) {
            for (String cover : covers()) {
                if (cover != null && !cover.equals("-1")) {
                    return cover;
                }
            }
        }
        return null;
    }

    public int olKey() {
        return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("W")));
    }

    public String olKeyString() {
        return key.substring(key.indexOf("OL") + 2, key.indexOf("W"));
    }

}
