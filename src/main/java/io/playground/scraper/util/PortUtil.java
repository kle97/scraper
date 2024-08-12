package io.playground.scraper.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class PortUtil {

    private PortUtil() {}
    
    public static boolean isPortFree(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) is required only on macOS, 
            // otherwise the code will not work correctly on that platform          
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
