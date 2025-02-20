package io.playground.scraper.openlibrary.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.neovisionaries.i18n.LocaleCode;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public record Edition(String title,
                      String subtitle,
                      @JsonProperty("description") JsonNode descriptionNode,
                      List<NodeKey> works,
                      String key,

                      String pagination,
                      Integer numberOfPages,
                      String physicalFormat,
                      String physicalDimensions,
                      String weight,

                      @JsonProperty("isbn_10") List<String> isbn10,
                      @JsonProperty("isbn_13") List<String> isbn13,
                      List<String> oclcNumbers,
                      List<String> lccn,
                      List<String> deweyDecimalClass,
                      List<String> lcClassifications,

                      List<Volumes> volumes,
                      List<NodeKey> languages,
                      List<String> publishers,
                      String publishDate,
                      String publishCountry,
                      List<String> publishPlaces,

                      String byStatement,
                      @JsonProperty("contributions") List<String> contributionsNode,
                      @JsonProperty("identifiers") Identifiers identifiersNode,

                      List<String> covers) {

    private static final Map<String, String> LOCALE_MAP = new HashMap<>();
    static {
        for (LocaleCode locale : LocaleCode.values()) {
            try {
                LOCALE_MAP.put(locale.getLanguage().getAlpha3().getAlpha3B().name(), locale.getLanguage().getName());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public String description() {
        if (descriptionNode != null) {
            if (descriptionNode.isTextual()) {
                return descriptionNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
            } else if (descriptionNode.isObject()) {
                return JacksonUtil.readValue(descriptionNode, NodeTypeValue.class)
                                  .value()
                                  .replaceAll("\\r|\\n|\\r\\n", " ");
            }
        }
        return null;
    }

    public String publisher() {
        if (publishers != null && !publishers.isEmpty()) {
            return publishers.getFirst();
        }
        return null;
    }

    public String language() {
        if (languages != null && !languages.isEmpty()) {
            String key = languages.getFirst().key();
            String code = key.substring(11);
            return LOCALE_MAP.getOrDefault(code, null);
        }
        return null;
    }

    public Integer numberOfVolumes() {
        if (volumes != null) {
            return volumes.size();
        }
        return null;
    }

    public String publishPlace() {
        if (publishPlaces != null && !publishPlaces.isEmpty()) {
            return publishPlaces.getFirst();
        }
        return null;
    }

    public String identifiers() {
        if (identifiersNode != null) {
            StringBuilder sb = new StringBuilder();
            if (identifiersNode.goodreads() != null && !identifiersNode.goodreads().isEmpty()) {
                sb.append("goodreads:").append(identifiersNode.goodreads().getFirst());
            }
            if (identifiersNode.librarything() != null && !identifiersNode.librarything().isEmpty()) {
                sb.append("librarything:").append(identifiersNode.librarything().getFirst());
            }
            return sb.toString();
        }
        return null;
    }

    public String lcClassification() {
        if (lcClassifications != null && !lcClassifications.isEmpty()) {
            return lcClassifications.getFirst();
        }
        return null;
    }

    public String deweyNumber() {
        if (deweyDecimalClass != null && !deweyDecimalClass.isEmpty()) {
            return deweyDecimalClass.getFirst();
        }
        return null;
    }

    public String lccnNumber() {
        if (lccn != null && !lccn.isEmpty()) {
            return lccn.getFirst();
        }
        return null;
    }

    public String oclcNumber() {
        if (oclcNumbers != null && !oclcNumbers.isEmpty()) {
            return oclcNumbers.getFirst();
        }
        return null;
    }

    public String contributions() {
        if (contributionsNode != null && !contributionsNode.isEmpty()) {
            return String.join("; ", contributionsNode());
        }
        return null;
    }

    public int olKey() {
        return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("M")));
    }

    public Integer workKey() {
        if (works != null && !works.isEmpty()) {
            String work = works.getFirst().key();
            return Integer.parseInt(work.substring(work.indexOf("OL") + 2, work.indexOf("W")));
        }
        return null;
    }
}
