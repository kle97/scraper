package io.playground.scraper.model.response.value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.playground.scraper.util.JacksonUtil;

import java.util.List;

public record SerializedValue(
        String type,
        JsonNode value
) {
    
    public List<SerializedValue> getValueAsNode() {
        if (value.isArray()) {
            return JacksonUtil.readValue(value, new TypeReference<List<SerializedValue>>() {});
        } else if (value.isObject()) {
            return List.of(JacksonUtil.convertValue(value, SerializedValue.class));
        }
        
        return List.of();
    }
    
    public String getValueAsString() {
        if (value.isTextual()) {
            return value.asText();
        }
        return "";
    }

    public Object getValue() {
        return value.deepCopy();
    }
}
