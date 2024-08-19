package io.playground.scraper.util;

import io.playground.scraper.model.chromedevtools.DevToolsPayload;
import io.playground.scraper.model.chromedevtools.DevToolsMethod;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ClientEndpoint
public class DevToolsClient {

    public static final int DEFAULT_TIMEOUT_IN_MS = 30000;
    private static final ConditionFactory await = Awaitility.waitAtMost(Duration.ofMillis(DEFAULT_TIMEOUT_IN_MS))
                                                            .pollDelay(Duration.ZERO)
                                                            .ignoreExceptions();
    private final String endpoint;
    private int requestId;
    private Session session;
    private final Map<Integer, DevToolsPayload> messages = new ConcurrentHashMap<>();
    private final List<DevToolsPayload> events = new ArrayList<>();

    public DevToolsClient(String endpoint) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
            this.endpoint = endpoint;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(endpoint));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("Connected to server {}!", endpoint);
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Received from ChromeDevTools: {}", message);
        try {
            DevToolsPayload payload = JacksonUtil.readValue(message, DevToolsPayload.class);
            if (payload.hasId()) {
                messages.put(payload.getId(), payload);
            } else if (payload.isEvent()) {
                events.add(payload);
            }
        } catch (Exception ignored) {
        }
    }

    public void send(String message) {
        session.getAsyncRemote().sendText(message);
        log.info("Sent message '{}' to server '{}'!", message, endpoint);
    }

    public DevToolsPayload sendAndWait(String message) {
        send(message);
        return await.until(() -> messages.get(requestId), Objects::nonNull);
    }

    public DevToolsPayload sendAndWait(DevToolsMethod method) {
        return sendAndWait(method, null);
    }

    public DevToolsPayload sendAndWait(DevToolsMethod method, Map<String, Object> params) {
        send(createPayload(method, params));
        return await.until(() -> messages.get(requestId), Objects::nonNull);
    }

    public boolean waitForEvent(DevToolsMethod targetEvent, int from) {
        return waitForEvent(targetEvent.getMethod(), from);
    }
    
    public boolean waitForEvent(String targetEvent, int from) {
        try {
            await.until(() -> {
                for (int i = events.size() - 1; i >= from; i--) {
                    DevToolsPayload event = events.get(i);
                    if (event.isEvent() && event.getMethod().equals(targetEvent)) {
                        return true;
                    }
                }
                return false;
            });
            return true;
        } catch (ConditionTimeoutException ignored) {
        }
        return false;
    }
    
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    public void close() {
        if (isOpen()) {
            try {
                session.close();
                log.info("Closed the session with server '{}'!", endpoint);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public DevToolsPayload getBrowserVersion() {
        return sendAndWait(DevToolsMethod.BROWSER_GET_VERSION);
    }

    public void enablePage() {
        sendAndWait(DevToolsMethod.PAGE_ENABLE);
    }

    public boolean navigate(String url) {
        boolean navigated = false;
        try {
            int lastEventIndex = events.size() - 1;
            sendAndWait(DevToolsMethod.PAGE_NAVIGATE, Map.of("url", url, "transitionType", "link"));
            waitForEvent(DevToolsMethod.PAGE_LOAD_EVENT_FIRED, lastEventIndex);
            navigated = true;
        } catch (ConditionTimeoutException e) {
            log.error(e.getMessage());
            
        }
        return navigated;
    }
    
    public int getRootNodeId() {
        DevToolsPayload payload = getDocument();
        if (payload.isResult()) {
            try {
                return JacksonUtil.getAsInteger(payload.getResult(), "root", "nodeId");
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
        return -1;
    }

    public String getCurrentFrameId() {
        DevToolsPayload payload = getFrameTree();
        if (payload.isResult()) {
            try {
                return JacksonUtil.getAsString(payload.getResult(), "frameTree", "frame", "id");
            } catch (Exception ignored) {
            }
        }
        return "";
    }
    
    public DevToolsPayload getDocument() {
        return sendAndWait(DevToolsMethod.DOM_GET_DOCUMENT, Map.of("pierce", true));
    }

    public DevToolsPayload getFrameTree() {
        return sendAndWait(DevToolsMethod.PAGE_GET_FRAME_TREE);
    }
    
    public int createIsolatedWorld(String frameId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.PAGE_CREATE_ISOLATED_WORLD, Map.of("frameId", frameId));
        if (payload.isResult()) {
            try {
                return JacksonUtil.getAsInteger(payload.getResult(), "executionContextId");
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    public String resolveNode(int nodeId, int executionContextId) {
        Map<String, Object> params = Map.of("nodeId", nodeId, "executionContextId", executionContextId);
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_RESOLVE_NODE, params);
        if (payload.isResult()) {
            try {
                return JacksonUtil.getAsString(payload.getResult(), "object", "objectId");
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    
    public String callFunctionOn(String script, String objectId, String value, int executionContextId) {
        String format = """
                    (function(...arguments) {
                    const obj = arguments.shift();
                    return %s;})""";
        script = String.format(format, script);

        List<Object> arguments = List.of(Map.of("objectId", objectId), Map.of("value", value));
        
        DevToolsPayload payload = sendAndWait(DevToolsMethod.RUNTIME_CALL_FUNCTION_ON, Map.ofEntries(
                Map.entry("executionContextId", executionContextId),
                Map.entry("functionDeclaration", script),
                Map.entry("arguments", arguments),
                Map.entry("useGesture", true),
                Map.entry("awaitPromise", false),
                Map.entry("generatePreview", true),
                Map.entry("serializationOptions", Map.of("serialization", "deep", "additionalParameters",
                                                         Map.of("includeShadowTree", "all", "maxNodeDepth", 2)))
        ));
        if (payload.isResult()) {
            try {
                return JacksonUtil.getAsString(payload.getResult(), "result", "objectId");
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    
    public int extractContextId(String objectId) {
        if (objectId != null && !objectId.isEmpty()) {
            String[] tokens = objectId.split("\\.");
            if (tokens.length > 1) {
                return Integer.parseInt(tokens[1]);
            }
        }
        return -1;
    }

    public String createPayload(DevToolsMethod method) {
        return createPayload(method, null);
    }

    public String createPayload(String method) {
        return createPayload(method, null);
    }

    public String createPayload(DevToolsMethod method, Map<String, Object> params) {
        return createPayload(method.getMethod(), params);
    }

    public String createPayload(String method, Map<String, Object> params) {
        DevToolsPayload payload = DevToolsPayload.builder()
                                                 .id(++requestId)
                                                 .method(method)
                                                 .params(params)
                                                 .build();
        return JacksonUtil.writeValueAsString(payload);
    }
}
