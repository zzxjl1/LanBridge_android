package com.zzxjl1.lanbridge;

import android.util.Log;
import android.widget.Toast;

import com.kongzue.dialogx.dialogs.PopTip;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class connections {

    public static HashMap<Object, Object> data = new HashMap<>();

    static void init_info(String ip) {
        battery.get_remote_battery_status(ip);
        set_sys_info(ip);
    }

    static void set_sys_info(String ip) {
        String url = String.format("http://%s:%d/api/sys_info", ip, 8000);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(new AsyncHttpGet(url), new AsyncHttpClient.JSONObjectCallback() {

            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                if (e != null) {
                    Log.e("get_remote_sys_info failed", e.toString());
                    return;
                }
                HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
                t.put("sys_info", result);
                data.put(ip, t);
                MainActivity.execJs("update_all_lan_clients()", null);
            }
        });
    }

    static JSONObject get_battery_status(String ip){
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        return (JSONObject) t.get("battery");
    }

    static void set_battery_status(String ip, JSONObject obj) {
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        t.put("battery", obj);
        data.put(ip, t);
        MainActivity.execJs("update_all_lan_clients()", null);
    }

    static boolean is_ws_connected(String ip) {
        if (!data.containsKey(ip)) return false;
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        return ((boolean) t.get("ws_connected") && t.get("ws_conn") != null);
    }

    static void set_ws_conn(String ip, WebSocket conn) {
        if (!data.containsKey(ip)) return;
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        t.put("ws_conn", conn);
        data.put(ip, t);
    }

    static void set_ws_connected(String ip) {
        if (!data.containsKey(ip)) return;
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        t.put("ws_connected", true);
        data.put(ip, t);
        Log.e("conns:", data.toString());
        /****** 下面开始连接建立后的事件 *****/
        init_info(ip);
        PopTip.show(String.format("与 %s 建立连接", ip)).autoDismiss(1000);

    }

    public static void set_ws_disconnected(String ip) {
        if (!data.containsKey(ip)) return;
        HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(ip);
        t.put("ws_connected", false);
        t.put("ws_conn", null);
        data.put(ip, t);
        Log.e("conns:", data.toString());
        /****** 下面开始连接断开后的事件 *****/
        MainActivity.execJs("update_all_lan_clients()", null);
        PopTip.show(String.format("与 %s 连接断开", ip)).autoDismiss(1000);

    }

    static void add(String ip) {
        if (data.containsKey(ip)) return;
        HashMap<Object, Object> t = new HashMap<>();
        t.put("birth_time", System.currentTimeMillis());
        t.put("ws_conn", null);
        t.put("ws_connected", false);
        t.put("battery", new HashMap<Object, Object>() {{
            put("has_battery", false);
        }});
        t.put("sys_info", new HashMap<>());
        data.put(ip, t);
    }

    static LinkedList<WebSocket> all_available_ws_conn() {
        LinkedList<WebSocket> result = new LinkedList<WebSocket>();
        Iterator iterator = data.keySet().iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            HashMap<Object, Object> t = (HashMap<Object, Object>) data.get(key);
            if ((boolean) t.get("ws_connected")) {
                result.add((WebSocket) t.get("ws_conn"));
            }
        }
        return result;
    }

    static int available_ws_conn_count() {
        return all_available_ws_conn().size();
    }

    public static void broadcast(String msg) {

        for (WebSocket conn : all_available_ws_conn()) {
            conn.send(msg);
            //Log.e("brocasted msg",msg);
        }
    }

}
