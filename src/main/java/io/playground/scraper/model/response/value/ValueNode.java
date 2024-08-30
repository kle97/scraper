package io.playground.scraper.model.response.value;

import java.util.List;
import java.util.Map;

public record ValueNode(
        String nodeType,
        
        Integer childNodeCount,
        
        Integer backendNodeId,
        
        String loaderId,

        String mode,

        SerializedValue shadowRoot,
        
        String localName,
        
        String namespaceURI,
        
        Map<String, String> attributes,
        
        List<SerializedValue> children
) {
}
