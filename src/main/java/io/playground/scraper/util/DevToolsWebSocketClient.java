package io.playground.scraper.util;

import io.playground.scraper.model.DevToolsMethod;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ClientEndpoint
public class DevToolsWebSocketClient {

    private int id;
    private Session session;

    public DevToolsWebSocketClient(String endpoint) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(endpoint));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("Scheduler connected to the server.");
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Scheduler received message from the server: {}", message);
    }

    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
        log.info("Scheduler sent message \"{}\" to the server.", message);
    }

    public void close() {
        try {
            session.close();
            log.info("Scheduler closed the session.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getBrowserVersion() {
        sendMessage(createPayload(DevToolsMethod.BROWSER_GET_VERSION, Map.of()));
    }

    public void querySelector(String selector) {
        sendMessage(createPayload(DevToolsMethod.DOM_QUERY_SELECTOR, Map.of("nodeId", 0, "selector", selector)));
    }

    public void enablePage() {
        sendMessage(createPayload(DevToolsMethod.PAGE_ENABLE, Map.of()));
    }

    public void getOuterHtml() {
        sendMessage(createPayload(DevToolsMethod.DOM_GET_OUTER_HTML, Map.of("nodeId", 0)));
    }

    public void getDocument() {
        getDocument(-1, true);
    }

    public void getDocument(int depth, boolean pierce) {
        sendMessage(createPayload(DevToolsMethod.DOM_GET_DOCUMENT, Map.of("depth", depth, "pierce", pierce)));
    }

    private String createPayload(DevToolsMethod method, Map<String, Object> params) {
        return createPayload(method.getMethod(), params);
    }

    private String createPayload(String method, Map<String, Object> params) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id++);
        payload.put("method", method);
        if (params != null && !params.isEmpty()) {
            payload.put("params", params);
        }
        return JacksonUtil.writeValueAsString(payload);
    }
}
