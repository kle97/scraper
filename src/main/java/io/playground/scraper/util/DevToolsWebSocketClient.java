package io.playground.scraper.util;

import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;

@Slf4j
@ClientEndpoint
public class DevToolsWebSocketClient {
    
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
}
