/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules.seedmap;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class SeedMapWebSocket extends WebSocketServer {

    private boolean onlyLocal;
    private boolean debugMessages;
    private final List<WebSocket> connections = new ArrayList<>();

    public SeedMapWebSocket(int port, boolean onlyLocal, boolean debugMessages) {
        super(new InetSocketAddress(port));
        this.onlyLocal = onlyLocal;
        this.debugMessages = debugMessages;
        setReuseAddr(true);
    }

    public boolean getOnlyLocal() {
        return onlyLocal;
    }

    public boolean getDebugMessages() {
        return debugMessages;
    }

    public void setOnlyLocal(boolean onlyLocal) {
        this.onlyLocal = onlyLocal;
        if (onlyLocal) {
            for (WebSocket conn : connections) {
                if (!getAddr(conn).equals("localhost") && conn.isOpen()) {
                    conn.close();
                }
            }
        }
    }

    public void setDebugMessages(boolean debugMessages) {
        this.debugMessages = debugMessages;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!Modules.get().get(SeedMap.class).isActive()) {
            conn.close();
            return;
        }
        if (onlyLocal && !getAddr(conn).equals("localhost")) {
            conn.close();
            return;
        }
        connections.add(conn);
        if (debugMessages) {
            ChatUtils.infoPrefix("SeedMap", "New connection from " + getAddr(conn) + ":" + conn.getRemoteSocketAddress().getPort());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        if (debugMessages) {
            ChatUtils.infoPrefix("SeedMap", "Closed connection to " + getAddr(conn) + ":" + conn.getRemoteSocketAddress().getPort());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (debugMessages) {
            ChatUtils.infoPrefix("SeedMap", getAddr(conn) + ":" + conn.getRemoteSocketAddress().getPort() + " : " + message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {}

    @Override
    public void onStart() {
        setConnectionLostTimeout(5);
    }

    public static String getAddr(WebSocket conn) {
        String addr = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        if (addr.equals("0:0:0:0:0:0:0:1") || addr.equals("127.0.0.1"))
            addr = "localhost";
        return addr;
    }

    public void closeAll() {
        for (WebSocket conn : connections) {
            conn.close();
        }
        connections.clear();
    }
}

