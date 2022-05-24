package com.zzxjl1.lanbridge;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;


import com.kongzue.dialogx.dialogs.PopTip;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class battery extends Service {
    static int percentage;
    static boolean is_charging;
    static boolean has_battery = false;
    private BroadcastReceiver batteryLevelRcvr;

    public static void parse_change(String ip, JSONObject old, JSONObject data) {
        try {
            if (old.toString().equals(data.toString())) return;
            boolean is_charging = data.getBoolean("is_charging");
            int percentage = data.getInt("percentage");
            if (old.getBoolean("is_charging") != is_charging) {
                PopTip.show(String.format("%s %s", ip, is_charging ? "接入电源" : "正使用电池")).autoDismiss(1000);
            }
            if (old.getInt("percentage") != percentage) {
                if (percentage < 20 && !is_charging) {
                    PopTip.show(String.format("%s 电池电量低(%d%%)", ip, percentage)).autoDismiss(1000);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        monitorBatteryState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryLevelRcvr);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    static void get_remote_battery_status(String ip) {
        String url = String.format("http://%s:%d/api/battery_status", ip, 8000);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(new AsyncHttpGet(url), new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                if (e != null) {
                    Log.e("get_remote_battery_status failed", e.toString());
                    return;
                }
                connections.set_battery_status(ip, result);
            }
        });
    }


    HashMap<Object, Object> last = null;

    void broadcast_battery_status() {
        HashMap<Object, Object> t = new HashMap<Object, Object>() {{
            put("action", "battery_status_update");
            put("data", battery_data());
        }};
        if (t.equals(last)) return;
        last = t;
        connections.broadcast(new JSONObject(t).toString());
    }

    static HashMap<Object, Object> battery_data() {
        return new HashMap<Object, Object>() {{
            put("has_battery", has_battery);
            put("percentage", percentage);
            put("is_charging", is_charging);
        }};
    }

    private void monitorBatteryState() {
        batteryLevelRcvr = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int status = intent.getIntExtra("status", -1);

                if (rawlevel >= 0 && scale > 0) {
                    percentage = (rawlevel * 100) / scale;
                }


                if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    has_battery = false;
                } else {
                    has_battery = true;
                }

                if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                    is_charging = true;
                } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    is_charging = false;
                }

                //Log.e("battery status changed", battery_data().toString());
                broadcast_battery_status();
            }

        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelRcvr, batteryLevelFilter);

    }


}