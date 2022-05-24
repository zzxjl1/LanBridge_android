package com.zzxjl1.lanbridge;

import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;

import org.json.JSONObject;

public class js_bridge {

    @JavascriptInterface
    public void toast(String t) {
        Toast.makeText(MyApplication.base, t, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public String get_all_lan_clients() {
        return new JSONObject(connections.data).toString();
    }

    @JavascriptInterface
    public void show_file_explorer(String ip) {
        Uri uri = Uri.parse(String.format("http://%s:%d/file_explorer", ip, 8000));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MyApplication.base.startActivity(intent);
    }

    @JavascriptInterface
    public boolean is_undisturb_on() {
        return false;
    }

    @JavascriptInterface
    public void toggle_undisturb_mode(boolean t) {

    }

    @JavascriptInterface
    public boolean is_soundwire_allowed() {
        return soundwire.is_allowed();
    }

    @JavascriptInterface
    public void toggle_soundwire(boolean t) {
        soundwire.toggle_allowed_switch(t);
    }

    @JavascriptInterface
    public boolean is_clipboard_share_running() {
        return false;
    }

    @JavascriptInterface
    public void toggle_clipboard_share(boolean t) {

    }

    @JavascriptInterface
    public void find_my_device(String ip) {
        String url = String.format("http://%s:%d/api/find_my_device", ip, 8000);
        AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(url), null);
    }

    @JavascriptInterface
    public void power_management(String ip, String action) {
        String url = String.format("http://%s:%d/api/%s", ip, 8000, action);
        System.out.println(url);
        AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(url), null);
    }


}
