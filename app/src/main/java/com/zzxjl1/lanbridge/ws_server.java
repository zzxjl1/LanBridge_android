package com.zzxjl1.lanbridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;

public class ws_server extends Service {

    WebSocketServer server = null;

    @Override
    public void onCreate() {
        super.onCreate();
        start_ws_server();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (WebSocket conn : connections.all_available_ws_conn()) {
            conn.close();
        }
//        if (server != null) {
//            try {
//                server.stop();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void start_ws_server() {
        Draft perMessageDeflateDraft = new Draft_6455(new PerMessageDeflateExtension());
        server = new WebSocketServer(new InetSocketAddress(8765), Collections.singletonList(perMessageDeflateDraft)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                Log.e("ws server", "连接建立");
                connections.add(ip);
                connections.set_ws_conn(ip, conn);
                connections.set_ws_connected(ip);
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                Log.e("ws server", "连接断开");
                connections.set_ws_disconnected(ip);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                Log.e("ws server on msg", message);
                ws_handler.parse(message, ip);
            }

            @Override
            public void onError(WebSocket conn, Exception e) {
                String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                Log.e("ws server error", e.toString());
                connections.set_ws_disconnected(ip);
            }

            @Override
            public void onStart() {
                Log.e("ws server", "启动成功");
            }
        };
        server.setReuseAddr(true);
        server.start();

    }
}