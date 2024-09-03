package io.playground.scraper.model.response.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Node(
        Integer nodeId,
        
        Integer backendNodeId,
        
        Integer nodeType,
        
        String nodeName,
        
        String localName,
        
        String nodeValue,
        
        Integer childNodeCount,
        
        String frameId,
        
        List<Node> children,
        
        List<Object> attributes,
        
        String publicId,
        String systemId,
        
        String documentURL,
        String baseURL,
        String xmlVersion,
        String compatibilityNode
) {
    
    public Map<String, Object> getAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        if (attributes != null) {
            for (int i = 0; i < attributes.size(); i+=2) {
                attrs.put((String) attributes.get(i), attributes.get(i + 1));
            }
        }
        return attrs;
    }
}
