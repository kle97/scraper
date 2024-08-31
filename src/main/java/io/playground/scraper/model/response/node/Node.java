package io.playground.scraper.model.response.node;

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
) {}
