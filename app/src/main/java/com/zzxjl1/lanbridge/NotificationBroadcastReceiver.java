package com.zzxjl1.lanbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case "STOP_FIND_MY_DEVICE":
                http_server.find_my_device_running = false;
                break;
        }

    }
}