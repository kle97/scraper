package io.playground.scraper.model.response;

import io.playground.scraper.model.response.value.SerializedValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ResolvedNode(
        
        String type,
        
        String subtype,
        
        String className,
        
        String description,
        
        SerializedValue deepSerializedValue,
        
        String objectId
) {
    
    public int getListSize() {
        if (description != null && description.contains("NodeList")) {
            try {
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(description);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group());
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
    
    public int getExecutionContextId() {
        if (objectId != null && !objectId.isEmpty()) {
            try {
                String[] tokens = objectId.split("\\.");
                if (tokens.length > 1) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }
}
