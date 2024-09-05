package io.playground.scraper.model.response.value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.util.JacksonUtil;

import java.util.List;

public record SerializedValue(
        String type,
        JsonNode value
) {
    
    public List<SerializedValue> getValueAsNode() {
        if (value.isArray()) {
            return JacksonUtil.readValue(value, new TypeReference<>() {});
        } else if (value.isObject()) {
            return List.of(JacksonUtil.convertValue(value, SerializedValue.class));
        }
        
        return List.of();
    }

    public boolean getValueAsBoolean() {
        try {
            if (value != null && value.isBoolean()) {
                return value.asBoolean();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public String getValueAsString() {
        try {
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        } catch (Exception ignored) {
        }
        return UCDriver.ELEMENT_NOT_FOUND;
    }

    public int getValueAsInteger() {
        try {
            if (value != null && value.isNumber()) {
                return value.asInt();
            }
        } catch (Exception ignored) {
        }
        return Integer.MIN_VALUE;
    }

    public Object getValue() {
        return value.deepCopy();
    }
}
