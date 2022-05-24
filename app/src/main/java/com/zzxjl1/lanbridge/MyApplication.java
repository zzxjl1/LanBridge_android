package com.zzxjl1.lanbridge;

import android.app.Application;
import android.content.Intent;


import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.style.IOSStyle;
import com.koushikdutta.ion.Ion;


public class MyApplication extends Application {
    public static MyApplication base;


    @Override
    public void onCreate() {
        super.onCreate();
        base = this;
        Ion.getDefault(this).getConscryptMiddleware().enable(false);
        DialogX.init(this);
        DialogX.globalStyle = new IOSStyle();
        //DialogX.globalStyle = new KongzueStyle();
        DialogX.globalTheme = DialogX.THEME.AUTO;
        DialogX.onlyOnePopTip = false;

        startService(new Intent(this, sniff.class));
        startService(new Intent(this, ws_server.class));
        startService(new Intent(this, http_server.class));
        startService(new Intent(this, battery.class));
        startService(new Intent(this, soundwire.class));
    }


}
