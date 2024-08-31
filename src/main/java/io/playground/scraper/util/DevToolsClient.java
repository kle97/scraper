package io.playground.scraper.util;

import io.playground.scraper.model.chromedevtools.DevToolsPayload;
import io.playground.scraper.model.chromedevtools.DevToolsMethod;
import io.playground.scraper.model.response.IsolateWorld;
import io.playground.scraper.model.response.ObjectNode;
import io.playground.scraper.model.response.ScriptNode;
import io.playground.scraper.model.response.ResolvedNode;
import io.playground.scraper.model.response.boxmodel.BoxModel;
import io.playground.scraper.model.response.boxmodel.Point;
import io.playground.scraper.model.response.frame.FrameTree;
import io.playground.scraper.model.response.node.RootNode;
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
        DevToolsPayload payload = await.until(() -> messages.get(requestId), Objects::nonNull);
        if (payload != null && payload.isError()) {
            log.error("code: {} - message: {}", payload.getError().code(), payload.getError().message());
        }
        return payload;
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

    private String globalThisId;
    private int rootNodeId;
    private String rootObjectId;
    private String currentFrameId;
    private int currentExecutionContextId;
    
    public int getRootNodeId() {
        if (rootNodeId == 0) {
            RootNode rootNode = getDocument();
            rootNodeId = rootNode != null ? rootNode.root().nodeId() : -1;   
        }
        return rootNodeId;
    }
    
    public RootNode getDocument() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_DOCUMENT, Map.of("pierce", true));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), RootNode.class);
        }
        return null;
    }

    public String getCurrentFrameId() {
        if (currentFrameId == null) {
            FrameTree frameTree = getFrameTree();
            currentFrameId = frameTree != null ? frameTree.frameTree().frame().id() : "";
        }
        return currentFrameId;
    }

    public FrameTree getFrameTree() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.PAGE_GET_FRAME_TREE);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), FrameTree.class);
        }
        return null;
    }
    
    public int getCurrentExecutionContextId() {
        if (currentExecutionContextId == 0) {
            IsolateWorld isolateWorld = createIsolatedWorld(getCurrentFrameId());
            if (isolateWorld != null) {
                currentExecutionContextId = isolateWorld.executionContextId();
            }
        }
        return currentExecutionContextId;
    }
    
    public IsolateWorld createIsolatedWorld(String frameId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.PAGE_CREATE_ISOLATED_WORLD, Map.of("frameId", frameId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), IsolateWorld.class);
        }
        return null;
    }

    public ObjectNode resolveNode(int nodeId, int executionContextId) {
        Map<String, Object> params = Map.of("nodeId", nodeId, "executionContextId", executionContextId);
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_RESOLVE_NODE, params);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), ObjectNode.class);
        }
        return new ObjectNode(new ResolvedNode("", "", null, "", "", null, ""));
    }
    
    public String getRootObjectId() {
        if (rootObjectId == null) {
            ObjectNode objectNode = resolveNode(getRootNodeId(), getCurrentExecutionContextId());
            if (objectNode != null) {
                rootObjectId = objectNode.object().objectId();
            }
        }
        return rootObjectId;
    }

    public ScriptNode getGlobalThis() {
        Map<String, Object> params = Map.of("expression", "globalThis", "serializationOptions", Map.of("serialization", "idOnly"));
        DevToolsPayload payload = sendAndWait(DevToolsMethod.RUNTIME_EVALUATE, params);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), ScriptNode.class);
        }
        return null;
    }

    public String getGlobalThisId() {
        if (globalThisId == null) {
            ScriptNode scriptNode = getGlobalThis();
            if (scriptNode != null) {
                globalThisId = scriptNode.result().objectId();
            }
        }
        return globalThisId;
    }

    public void scrollIntoViewIfNeeded(String objectId) {
        sendAndWait(DevToolsMethod.DOM_SCROLL_INTO_VIEW_IF_NEEDED, Map.of("objectId", objectId));
    }

    public BoxModel getBoxModel(String objectId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_BOX_MODEL, Map.of("objectId", objectId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), BoxModel.class);
        }
        return null;
    }

    public void moveMouse(Point point) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "mouseMoved");
        params.put("x", point.x());
        params.put("y", point.y());
//        params.put("modifiers", 0);
        params.put("button", "left");
        params.put("clickCount", 0);
//        params.put("force", 0);
//        params.put("tangentialPressure", 0);
//        params.put("tiltX", 0);
//        params.put("tiltY", 0);
//        params.put("twist", 0);
//        params.put("deltaX", 0);
//        params.put("deltaY", 0);
//        params.put("pointerType", "mouse");
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_MOUSE_EVENT, params);
    }

    public void clickMouse(Point point) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "mousePressed");
        params.put("x", point.x());
        params.put("y", point.y());
//        params.put("modifiers", 0);
        params.put("button", "left");
        params.put("clickCount", 1);
//        params.put("force", 0);
//        params.put("tangentialPressure", 0);
//        params.put("tiltX", 0);
//        params.put("tiltY", 0);
//        params.put("twist", 0);
//        params.put("deltaX", 0);
//        params.put("deltaY", 0);
//        params.put("pointerType", "mouse");
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_MOUSE_EVENT, params);

        params.put("type", "mouseReleased");
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_MOUSE_EVENT, params);
    }

    public void sendKey(char ch) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "keyDown");
        params.put("text", ch);
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_KEY_EVENT, params);

        params.put("type", "keyUp");
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_KEY_EVENT, params);
    }

    public ScriptNode executeAsyncScript(String script, int executionContextId, Map<String, Object> arguments) {
        String format = """
                    (function(...arguments) {
                        const obj = this;
                        const promise = new Promise((resolve, reject) => {arguments.push(resolve)});
                        %s;
                        return promise;
                    })""";
        script = String.format(format, script);

        List<Object> args = new ArrayList<>();
        if (arguments != null && !arguments.isEmpty()) {
            args.add(arguments);
        }

        return callFunctionOn(script, executionContextId, true, args.toArray());
    }

    public ScriptNode executeScript(String script, int executionContextId, String objectId) {
        return executeScript(script, executionContextId, objectId, null);
    }
    
    public ScriptNode executeScript(String script, int executionContextId, String objectId, Map<String, Object> arguments) {
        String format = """
                    (function(...arguments) {
                        const obj = arguments.shift();
                        %s;
                    })""";
        script = String.format(format, script);

        List<Object> args = new ArrayList<>();
        args.add(Map.of("objectId", objectId));
        if (arguments != null && !arguments.isEmpty()) {
            args.add(arguments);
        }

        return callFunctionOn(script, executionContextId, false, args.toArray());
    }

    public ScriptNode callFunctionOn(String script, int executionContextId, boolean awaitPromise, Object... args) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.RUNTIME_CALL_FUNCTION_ON, Map.ofEntries(
                Map.entry("executionContextId", executionContextId),
                Map.entry("functionDeclaration", script),
                Map.entry("arguments", args),
                Map.entry("useGesture", true),
                Map.entry("awaitPromise", awaitPromise),
                Map.entry("generatePreview", true),
                Map.entry("serializationOptions", Map.of("serialization", "deep", "additionalParameters",
                                                         Map.of("includeShadowTree", "all", "maxNodeDepth", 2)))
        ));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), ScriptNode.class);
        }
        return null;
    }
}
