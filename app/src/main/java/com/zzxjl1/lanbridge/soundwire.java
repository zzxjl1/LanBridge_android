package com.zzxjl1.lanbridge;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.util.Log;

import com.kongzue.dialogx.dialogs.PopTip;


public class soundwire extends Service {
    static int notifacation_id = 2;
    static String mode = "balance";
    static int SAMPLE_RATE = 22050;
    static int BLOCK_SIZE = 2048;
    static AudioTrack track = null;
    static boolean allowed_switch = false;

    static boolean is_allowed() {
        return allowed_switch;
    }

    static void toggle_allowed_switch(boolean t) {
        if (is_running() && !t) {
            stop_soundwire_client();
        }
        allowed_switch = t;
    }

    static boolean is_running() {
        return track != null;
    }

    static void set_mode(String t) {
        stop_soundwire_client();
        int factor = 1;
        mode = t;
        switch (t) {
            case "quality":
                factor = 4;
                break;
            case "balance":
                factor = 2;
                break;
            case "performance":
                factor = 1;
                break;
        }
        SAMPLE_RATE = 11025 * factor;
        BLOCK_SIZE = 1024 * factor;
        Log.e("soundwire mode set to", mode);
    }

    static String get_mode() {
        return mode;
    }

    static long last_frame_timestamp;

    static void add_to_pcm_queue(short[] t) {
        track.write(t, 0, t.length);
        last_frame_timestamp = System.currentTimeMillis();
    }

    static void start_soundwire_client() {
        if (!soundwire.is_allowed()) return;
        Log.e("soundwire", "starting client");
        track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_DEFAULT,
                AudioFormat.ENCODING_DEFAULT, BLOCK_SIZE, AudioTrack.MODE_STREAM);
        track.play();
        toolutils.show_notification("SOUNDWIRE 正在接收音频广播", "", notifacation_id);
        PopTip.show(String.format("SOUNDWIRE 正在接收音频广播")).autoDismiss(1500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (is_running()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (System.currentTimeMillis() - last_frame_timestamp > 2000) {
                        stop_soundwire_client();
                    }
                }
            }
        }).start();
    }

    static void stop_soundwire_client() {
        if (track == null) return;
        track.stop();
        track.flush();
        track.release();
        track = null;
        Log.e("soundwire", "client stoped");
        toolutils.cancel_notification(notifacation_id);
        PopTip.show(String.format("SOUNDWIRE 音频接收端已休眠")).autoDismiss(1500);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        set_mode("balance");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop_soundwire_client();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}