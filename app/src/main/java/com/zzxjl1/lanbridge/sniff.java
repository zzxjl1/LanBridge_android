package com.zzxjl1.lanbridge;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.UUID;


public class sniff extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock MulticastLock = wifi.createMulticastLock("sniff_lock");
        MulticastLock.acquire();
        WifiManager.WifiLock WifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wifi_lock");
        WifiLock.acquire();
        Log.e("WifiLock", "created");

        start_broadcast_receiver();
        start_broadcast_sender();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop_broadcast_receiver();
        //stop_broadcast_sender();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int interval = 2000;
    int port = 65535;
    String uuid_str = UUID.randomUUID().toString();
    boolean broadcast_sender_switch = false;
    boolean broadcast_receiver_switch = false;

    public void start_broadcast_sender() {
        broadcast_sender_switch = true;
        Thread sniff_broadcast_sender_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    socket.setReuseAddress(true);
                    while (broadcast_sender_switch) {
                        String messageStr = new JSONObject(new HashMap<Object, Object>() {{
                            put("uuid", uuid_str);
                            put("timestamp", System.currentTimeMillis());
                            put("udp_broadcast_interval_in_ms", interval);
                        }}).toString();
                        byte[] sendData = messageStr.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
                        socket.send(sendPacket);
                        Log.e("网络发现广播sender", "packet sent");
                        Thread.sleep(interval);
                    }
                } catch (Exception e) {
                    Log.e("网络发现广播sender出错", e.toString());
                }
            }
        });
        sniff_broadcast_sender_thread.start();
    }

    public void stop_broadcast_sender() {
        broadcast_sender_switch = false;
    }

    public void start_broadcast_receiver() {
        broadcast_receiver_switch = true;
        Thread sniff_broadcast_receiver_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(port);
                    socket.setBroadcast(true);
                    socket.setReuseAddress(true);
                    //socket.setSoTimeout(2000);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    Log.e("网络发现广播receiver", "started");
                    while (broadcast_receiver_switch) {
                        socket.receive(packet);
                        String target_ip = packet.getAddress().getHostAddress();
                        String data = new String(packet.getData(), packet.getOffset(), packet.getLength());
                        Log.e("网络发现广播received", data);
                        String ip = toolutils.get_ip();
                        if (ip.equals(target_ip)) continue;
                        connections.add(target_ip);
                        if (connections.is_ws_connected(target_ip)) continue;
                        WebSocketClient conn = create_ws_conn(target_ip);
                        connections.set_ws_conn(target_ip, conn.getConnection());
                    }
                } catch (Exception e) {
                    Log.e("网络发现广播receiver出错", e.toString());
                }
            }

            private WebSocketClient create_ws_conn(String ip) {
                String url = String.format("ws://%s:%d", ip, 8765);
                WebSocketClient conn = new WebSocketClient(URI.create(url), new Draft_6455(new PerMessageDeflateExtension())) {

                    @Override
                    public void onMessage(String message) {
                        Log.e("ws client on msg", message);
                        ws_handler.parse(message, ip);
                    }

                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        Log.e("ws client", "连接建立");
                        connections.set_ws_connected(ip);
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        Log.e("ws client", "连接断开");
                        connections.set_ws_disconnected(ip);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("ws client error", e.toString());
                        connections.set_ws_disconnected(ip);
                    }
                };

                conn.connect();
                return conn;

            }

        });
        sniff_broadcast_receiver_thread.start();
    }

    public void stop_broadcast_receiver() {
        broadcast_receiver_switch = false;
    }
}