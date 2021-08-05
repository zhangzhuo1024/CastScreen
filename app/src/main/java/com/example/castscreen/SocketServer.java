package com.example.castscreen;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class SocketServer extends WebSocketServer {
    private final String TAG = "SocketServer";
    private WebSocket webSocket;

    public SocketServer(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.e(TAG, "SocketServer onOpen");
        this.webSocket = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.e(TAG, "SocketServer reason = " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.e(TAG, "SocketServer onMessage = " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "SocketServer onError = " + ex);
    }

    @Override
    public void onStart() {
        Log.e(TAG, "SocketServer onStart");
    }

    public void sendData(byte[] bytes) {
        Log.e(TAG, "SocketServer sendData = " + bytes.length);
        if (webSocket != null && webSocket.isOpen()) {
            //通过WebSocket 发送数据
            webSocket.send(bytes);
        }
    }

    public void close() {
        Log.e(TAG, "SocketServer close");
        webSocket.close();
    }
}


