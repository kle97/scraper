package io.playground.scraper.core;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.model.chromedevtools.BrowserInfo;
import io.playground.scraper.model.chromedevtools.DevToolsMethod;
import io.playground.scraper.model.chromedevtools.DevToolsPayload;
import io.playground.scraper.model.response.IsolateWorld;
import io.playground.scraper.model.response.ObjectNode;
import io.playground.scraper.model.response.ResolvedNode;
import io.playground.scraper.model.response.ScriptNode;
import io.playground.scraper.model.response.boxmodel.BoxModel;
import io.playground.scraper.model.response.boxmodel.Point;
import io.playground.scraper.model.response.boxmodel.Rect;
import io.playground.scraper.model.response.cookie.CookieParams;
import io.playground.scraper.model.response.css.CSSStyle;
import io.playground.scraper.model.response.css.ComputedStyle;
import io.playground.scraper.model.response.frame.FrameTree;
import io.playground.scraper.model.response.history.NavigationHistory;
import io.playground.scraper.model.response.html.OuterHtml;
import io.playground.scraper.model.response.node.Node;
import io.playground.scraper.model.response.node.RootNode;
import io.playground.scraper.model.response.screenshot.ScreenshotData;
import io.playground.scraper.model.response.screenshot.ViewPort;
import io.playground.scraper.model.response.target.TargetInfo;
import io.playground.scraper.model.response.target.TargetInfoProp;
import io.playground.scraper.model.response.target.TargetInfos;
import io.playground.scraper.model.response.window.WindowBounds;
import io.playground.scraper.model.response.window.WindowInfo;
import io.playground.scraper.util.JacksonUtil;
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
    private final boolean logEvents;
    private final UCDriverOptions ucDriverOptions;
    private int requestId;
    private Session session;
    private final Map<Integer, DevToolsPayload> messages = new ConcurrentHashMap<>();
    private final List<DevToolsPayload> events = new ArrayList<>();
    private final List<String> dialogMessages = new ArrayList<>();

    private String globalThisId;
    private int rootNodeId;
    private String rootObjectId;
    private String currentFrameId;
    private int currentExecutionContextId;

    public DevToolsClient(String endpoint) {
        this(endpoint, UCDriverOptions.builder().build());
    }

    public DevToolsClient(String endpoint, UCDriverOptions ucDriverOptions) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
            this.endpoint = endpoint;
            this.ucDriverOptions = ucDriverOptions;
            this.logEvents = ucDriverOptions.isLogCDPEvents();
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
        if (logEvents) {
            if (message.length() < 1000) {
                log.info("Received from ChromeDevTools: {}", message);
            } else {
                log.info("Received from ChromeDevTools: {}...", message.substring(0, 1000));
            }
        }
        try {
            DevToolsPayload payload = JacksonUtil.readValue(message, DevToolsPayload.class);
            if (payload.isError() || message.contains("exceptionDetails")) {
                log.error("ErrorMessage[{}]", message);
            }
            
            if (payload.hasId()) {
                messages.put(payload.getId(), payload);
            } else if (payload.isEvent()) {
                if (payload.getMethod().contains("Fetch") && payload.hasParam() && payload.getParams().containsKey("requestId")) {
                    String requestId = (String) payload.getParams().get("requestId");
                    if (payload.getMethod().equals(DevToolsMethod.FETCH_AUTH_REQUIRED.getMethod())) {
                        fetchContinueWithAuth(requestId, ucDriverOptions.getProxyUsername(), ucDriverOptions.getProxyPassword());
                    } else {
                        fetchContinueRequest(requestId);
                    }
                    return;
                }
                
                events.add(payload);
                if (payload.getMethod().equals(DevToolsMethod.PAGE_LOAD_EVENT_FIRED.getMethod()) 
//                        || payload.getMethod().equals(DevToolsMethod.PAGE_FRAME_STOPPED_LOADING.getMethod())
//                        || payload.getMethod().equals(DevToolsMethod.DOM_CHILD_NODE_COUNT_UPDATED.getMethod())
                ) {
                    resetContext();
                } else if (payload.getMethod().equals(DevToolsMethod.PAGE_JAVASCRIPT_DIALOG_OPENING.getMethod())) {
                    try {
                        dialogMessages.add((String) payload.getResult().get("message"));
                    } catch (Exception ignored){
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void send(String message) {
        session.getAsyncRemote().sendText(message);
        if (logEvents) {
            log.info("Sent message '{}' to server '{}'!", message, endpoint);
        }
    }

    public void send(DevToolsMethod method, Map<String, Object> params) {
        send(createPayload(method, params));
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
        return waitForEvent(targetEvent.getMethod(), from, false);
    }

    public boolean waitForEvent(DevToolsMethod targetEvent, int from, boolean waitForStability) {
        return waitForEvent(targetEvent.getMethod(), from, waitForStability);
    }
    
    public boolean waitForEvent(String targetEvent, int from, boolean waitForStability) {
        try {
            await.until(() -> {
                for (int i = events.size() - 1; i >= from; i--) {
                    DevToolsPayload event = events.get(i);
                    if (event.isEvent() && event.getMethod().equals(targetEvent)) {
                        return !waitForStability || waitForLoadingEventToStop();
                    }
                }
                return false;
            });
            return true;
        } catch (ConditionTimeoutException ignored) {
        }
        return false;
    }

    public boolean waitForLoadingEventToStop() {
        return waitForLoadingEventToStop(1000);
    }
    
    public boolean waitForLoadingEventToStop(int delayInMs) {
        try {
            int eventSize = events.size();
            return Awaitility.waitAtMost(Duration.ofMillis(delayInMs + 100))
                             .pollDelay(Duration.ofMillis(delayInMs))
                             .pollInterval(Duration.ofMillis(30))
                             .until(() -> events.size() == eventSize, result -> result);
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
    
    public String getLastDialogMessage() {
        if (!dialogMessages.isEmpty()) {
            return dialogMessages.getLast();
        }
        return "NO_MESSAGES_AVAILABLE";
    }
    
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    public void quit() {
        if (isOpen()) {
            try {
//                closeBrowser();
                session.close();
                log.info("Closed the session with server '{}'!", endpoint);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void closeBrowser() {
        sendAndWait(DevToolsMethod.BROWSER_CLOSE);
    }

    public BrowserInfo getBrowserVersion() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.BROWSER_GET_VERSION);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), BrowserInfo.class);
        }
        return null;
    }
    
    public void emulationSetTimeZoneOverride(String timezoneId) {
        sendAndWait(DevToolsMethod.EMULATION_SET_TIME_ZONE_OVERRIDE, Map.of("timezoneId", timezoneId));
    }
    
    public WindowInfo getWindowForTarget(String targetId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.BROWSER_GET_WINDOW_FOR_TARGET, Map.of("targetId", targetId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), WindowInfo.class);
        }
        return null;
    }

    public WindowBounds getWindowBounds(String targetId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.BROWSER_GET_WINDOW_BOUNDS, Map.of("targetId", targetId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), WindowBounds.class);
        }
        return null;
    }

    public void setWindowBounds(String targetId, WindowBounds bounds) {
        WindowBounds windowBounds = getWindowBounds(targetId);
        if (windowBounds != null) {
            sendAndWait(DevToolsMethod.BROWSER_SET_WINDOW_BOUNDS, Map.of("targetId", targetId, "bounds", bounds));
        }
    }

    public void setCookie(CookieParams params) {
        Map<String, Object> paramMap = JacksonUtil.convertValue(params, new TypeReference<>() {});
        sendAndWait(DevToolsMethod.NETWORK_SET_COOKIE, paramMap);
    }

    public void deleteCookie(CookieParams params) {
        Map<String, Object> paramMap = JacksonUtil.convertValue(params, new TypeReference<>() {});
        sendAndWait(DevToolsMethod.NETWORK_DELETE_COOKIES, paramMap);
    }

    public CookieParams getCookies() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.NETWORK_GET_COOKIES);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), CookieParams.class);
        }
        return null;
    }

    public void deleteAllCookies() {
        sendAndWait(DevToolsMethod.NETWORK_CLEAR_BROWSER_COOKIES);
    }

    public void enablePage() {
        sendAndWait(DevToolsMethod.PAGE_ENABLE);
    }

    public boolean navigate(String url) {
        boolean navigated = false;
        try {
            int lastEventIndex = events.size() - 1;
            sendAndWait(DevToolsMethod.PAGE_NAVIGATE, Map.of("url", url, "transitionType", "link"));
//            waitForEvent(DevToolsMethod.PAGE_LOAD_EVENT_FIRED, lastEventIndex);
            waitForEvent(DevToolsMethod.PAGE_FRAME_STOPPED_LOADING, lastEventIndex, true);
            navigated = true;
        } catch (ConditionTimeoutException e) {
            log.error(e.getMessage());
        }
        return navigated;
    }
    
    public void enableFetch() {
        enableFetch("*");
    }

    public void enableFetch(String pattern) {
        sendAndWait(DevToolsMethod.FETCH_ENABLE, Map.of("patterns", List.of(Map.of("urlPattern", pattern)), "handleAuthRequests", true));
    }

    public void fetchContinueResponse(String requestId) {
        send(DevToolsMethod.FETCH_CONTINUE_RESPONSE, Map.of("requestId", requestId));
    }

    public void fetchContinueRequest(String requestId) {
        send(DevToolsMethod.FETCH_CONTINUE_REQUEST, Map.of("requestId", requestId));
    }

    public void fetchContinueWithAuth(String requestId, String username, String password) {
        send(DevToolsMethod.FETCH_CONTINUE_WITH_AUTH, Map.of("requestId", requestId, "authChallengeResponse", 
                                                                  Map.of("response", "ProvideCredentials",
                                                                         "username", username,
                                                                         "password", password)));
    }

    public void close() {
        sendAndWait(DevToolsMethod.TARGET_CLOSE_TARGET, Map.of("targetId", getCurrentFrameId()));
    }

    public Rect getRect(String objectId) {
        String script = """
                        let clientRect = obj.getBoundingClientRect();
                        let rect = {
                            x: clientRect.x,
                            y: clientRect.y,
                            width: clientRect.width,
                            height: clientRect.height,
                            scrollLeft: obj.scrollLeft,
                            scrollTop: obj.scrollTop,
                            clientLeft: obj.clientLeft,
                            clientTop: obj.clientTop,
                            clientWidth: obj.clientWidth,
                            clientHeight: obj.clientHeight,
                        };
                        return rect;
                        """;
        ScriptNode scriptNode = executeScript(script, objectId, "json");
        if (scriptNode != null) {
            return JacksonUtil.convertValue(scriptNode.result().value(), Rect.class);
        }
        return null;
    }
    
    public void reloadPage() {
        sendAndWait(DevToolsMethod.PAGE_RELOAD, Map.of("ignoreCache", true));
    }
    
    public void forwardPage() {
        NavigationHistory navigationHistory = getNavigationHistory();
        if (navigationHistory != null) {
            int targetIndex = Math.max(0, navigationHistory.currentIndex() + 1);
            navigateToHistoryEntry(navigationHistory.entries().get(targetIndex).id());
        }
    }

    public void backPage() {
        NavigationHistory navigationHistory = getNavigationHistory();
        if (navigationHistory != null) {
            int targetIndex = Math.max(0, navigationHistory.currentIndex() - 1);
            navigateToHistoryEntry(navigationHistory.entries().get(targetIndex).id());
        }
    }

    public NavigationHistory getNavigationHistory() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.PAGE_GET_NAVIGATION_HISTORY);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), NavigationHistory.class);
        }
        return null;
    }

    public void navigateToHistoryEntry(int entryId) {
        sendAndWait(DevToolsMethod.PAGE_NAVIGATE_TO_HISTORY_ENTRY, Map.of("entryId", Math.max(0, entryId)));
    }

    public void handleJavaScriptDialog(boolean accept) {
        handleJavaScriptDialog(accept, null);
    }
    
    public void handleJavaScriptDialog(boolean accept, String promptText) {
        Map<String, Object> params = new HashMap<>();
        params.put("accept", accept);
        if (promptText != null) {
            params.put("promptText", promptText);
        }
        sendAndWait(DevToolsMethod.PAGE_HANDLE_JAVASCRIPT_DIALOG, params);
    }

    public TargetInfos getTargets() {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.TARGET_GET_TARGETS);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), TargetInfos.class);
        }
        return null;
    }
    
    public TargetInfo getTargetInfo(String targetId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.TARGET_GET_TARGET_INFO, Map.of("targetId", targetId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), TargetInfo.class);
        }
        return null;
    }

    public boolean activateTarget(String targetId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.TARGET_ACTIVATE_TARGET, Map.of("targetId", targetId));
        attachToTarget(targetId);
        return payload.isResult();
    }
    
    public String createTarget(String url, boolean newWindow) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        params.put("newWindow", newWindow);
        DevToolsPayload payload = sendAndWait(DevToolsMethod.TARGET_CREATE_TARGET, params);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), TargetInfoProp.class).targetId();
        }
        return "";
    }

    public boolean attachToTarget(String targetId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.TARGET_ATTACH_TO_TARGET, Map.of("targetId", targetId));
        return payload.isResult();
    }
    
    public void setAutoAttach() {
        sendAndWait(DevToolsMethod.TARGET_SET_AUTO_ATTACH, Map.of("autoAttach", true, 
                                                                  "waitForDebuggerOnStart", true,
                                                                  "flatten", true));
    }
    
    public void resetContext() {
        globalThisId = null;
        rootNodeId = 0;
        rootObjectId = null;
        currentFrameId = null;
        currentExecutionContextId = 0;
    }
    
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
    
    public OuterHtml getOuterHtml(int nodeId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_OUTER_HTML, Map.of("nodeId", nodeId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), OuterHtml.class);
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

    public Integer requestNode(String objectId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_REQUEST_NODE, Map.of("objectId", objectId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), Node.class).nodeId();
        }
        return -1;
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

    public void focus(Integer nodeId) {
        focus(nodeId, null);
    }

    public void focus(String objectId) {
        focus(null, objectId);
    }
    
    public void focus(Integer nodeId, String objectId) {
        Map<String, Object> params = new HashMap<>();
        if (nodeId != null) {
            params.put("nodeId", nodeId);
        }
        if (objectId != null) {
            params.put("objectId", objectId);
        }
        sendAndWait(DevToolsMethod.DOM_FOCUS, params);
    }

    public void scrollIntoViewIfNeeded(Integer nodeId) {
        scrollIntoViewIfNeeded(nodeId, null, null);
    }

    public void scrollIntoViewIfNeeded(String objectId) {
        scrollIntoViewIfNeeded(null, objectId, null);
    }

    public void scrollIntoViewIfNeeded(Integer nodeId, String objectId, Rect rect) {
        Map<String, Object> params = new HashMap<>();
        if (nodeId != null) {
            params.put("nodeId", nodeId);
        }
        if (objectId != null) {
            params.put("objectId", objectId);
        }
        if (rect != null) {
            params.put("rect", rect);
        }
        sendAndWait(DevToolsMethod.DOM_SCROLL_INTO_VIEW_IF_NEEDED, params);
    }

    public BoxModel getBoxModel(String objectId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_BOX_MODEL, Map.of("objectId", objectId));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), BoxModel.class);
        }
        return null;
    }
    
    public Map<String, Object> getAttributes(int nodeId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_ATTRIBUTES, Map.of("nodeId", nodeId));
        if (payload != null) {
            return JacksonUtil.convertValue(payload.getResult(), Node.class).getAttributes();
        }
        return new HashMap<>();
    }
    
    public List<CSSStyle> getCSSStyle(int nodeId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.CCS_GET_COMPUTED_STYLE_FOR_NODE, Map.of("nodeId", nodeId));
        if (payload != null) {
            return JacksonUtil.convertValue(payload.getResult(), ComputedStyle.class).computedStyle();
        }
        return new ArrayList<>();
    }
    
    
    public Integer getContainerForNode(int nodeId) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.DOM_GET_CONTAINER_FOR_NODE, Map.of("nodeId", nodeId));
        if (payload != null) {
            return JacksonUtil.convertValue(payload.getResult(), Node.class).nodeId();
        }
        return 1;
    }
    
    public void scrollGesture(int x, int y, Integer xDistance, Integer yDistance, Integer speed, Integer repeatCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("x", x);
        params.put("y", y);
        if (xDistance != null) {
            params.put("xDistance", xDistance);
        }
        if (yDistance != null) {
            params.put("yDistance", yDistance);
        }
        if (speed != null) {
            params.put("speed", speed);
        }
        if (repeatCount != null) {
            params.put("repeatCount", repeatCount);
            params.put("repeatDelayMs", 0);
        }
        sendAndWait(DevToolsMethod.INPUT_SYNTHESIZE_SCROLL_GESTURE, params);
    }

    public void moveMouse(Point point) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "mouseMoved");
        params.put("x", point.x());
        params.put("y", point.y());
        params.put("button", "left");
        params.put("clickCount", 0);
        sendAndWait(DevToolsMethod.INPUT_DISPATCH_MOUSE_EVENT, params);
    }

    public void clickMouse(Point point) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "mousePressed");
        params.put("x", point.x());
        params.put("y", point.y());
        params.put("button", "left");
        params.put("clickCount", 1);
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

    public ScreenshotData getScreenshot() {
        return getScreenshot("png", null);
    }

    public ScreenshotData getScreenshot(String format) {
        return getScreenshot(format, null);
    }

    public ScreenshotData getScreenshot(ViewPort viewPort) {
        return getScreenshot("png", viewPort);
    }
    
    public ScreenshotData getScreenshot(String format, ViewPort viewPort) {
        Map<String, Object> params = new HashMap<>();
        params.put("format", format);
        if (viewPort != null) {
            params.put("clip", viewPort);
        }
        DevToolsPayload payload = sendAndWait(DevToolsMethod.PAGE_CAPTURE_SCREENSHOT, params);
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), ScreenshotData.class);
        }
        return null;
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

    public ScriptNode executeScript(String script, String objectId) {
        return executeScript(script, objectId, "deep");
    }

    public ScriptNode executeScript(String script, String objectId, String serialization) {
        return executeScript(script, getCurrentExecutionContextId(), objectId, serialization);
    }

    public ScriptNode executeScript(String script, int executionContextId, String objectId) {
        return executeScript(script, executionContextId, objectId, "deep");
    }

    public ScriptNode executeScript(String script, int executionContextId, String objectId, String serialization) {
        return executeScript(script, executionContextId, objectId, serialization, null);
    }

    public ScriptNode executeScript(String script, String objectId, Map<String, Object> arguments) {
        return executeScript(script, objectId, "deep", arguments);
    }

    public ScriptNode executeScript(String script, String objectId, String serialization, Map<String, Object> arguments) {
        return executeScript(script, getCurrentExecutionContextId(), objectId, serialization, arguments);
    }

    public ScriptNode executeScript(String script, int executionContextId, String objectId, Map<String, Object> arguments) {
        return executeScript(script, executionContextId, objectId, "deep", arguments);
    }
    
    public ScriptNode executeScript(String script, int executionContextId, String objectId, 
                                    String serialization, Map<String, Object> arguments) {
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

        return callFunctionOn(script, executionContextId, false, serialization, args.toArray());
    }

    public ScriptNode callFunctionOn(String script, int executionContextId, boolean awaitPromise, Object... args) {
        return callFunctionOn(script, executionContextId, awaitPromise, "deep", args);
    }

    public ScriptNode callFunctionOn(String script, int executionContextId, boolean awaitPromise, String serialization, Object... args) {
        DevToolsPayload payload = sendAndWait(DevToolsMethod.RUNTIME_CALL_FUNCTION_ON, Map.ofEntries(
                Map.entry("executionContextId", executionContextId),
                Map.entry("functionDeclaration", script),
                Map.entry("arguments", args),
                Map.entry("useGesture", true),
                Map.entry("awaitPromise", awaitPromise),
                Map.entry("generatePreview", true),
                Map.entry("serializationOptions", Map.of("serialization", serialization, "additionalParameters",
                                                         Map.of("includeShadowTree", "all", "maxNodeDepth", 2)))
        ));
        if (payload.isResult()) {
            return JacksonUtil.convertValue(payload.getResult(), ScriptNode.class);
        }
        return null;
    }
}
