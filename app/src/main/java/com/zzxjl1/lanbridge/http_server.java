package com.zzxjl1.lanbridge;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;


import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.stream.OutputStreamDataCallback;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class http_server extends Service {

    AsyncHttpServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        start_http_server();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (server != null) {
//            server.stop();
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static boolean find_my_device_running = false;
    int find_my_device_duration = 30;

    void start_http_server() {
        server = new AsyncHttpServer();

        server.get("/api/ping", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                toolutils.show_notification("debug", "pong", null, null, 100, false);
                response.send("pong");
            }
        });
        server.get("/api/sys_info", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send(new JSONObject(toolutils.get_sys_info()));
            }
        });

        server.get("/api/battery_status", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send(new JSONObject(battery.battery_data()));
            }
        });

        server.get("/api/find_my_device", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                if (find_my_device_running) {
                    response.send("");
                    return;
                }
                find_my_device_running = true;
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                MediaPlayer mPlayer = MediaPlayer.create(MyApplication.base, R.raw.ring);
                mPlayer.setLooping(true);
                mPlayer.start();
                long t = System.currentTimeMillis();

                NotificationManager notificationManager = (NotificationManager) MyApplication.base.getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("channel", "notify", NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.base);

                Intent intent = new Intent(MyApplication.base, NotificationBroadcastReceiver.class);
                intent.setAction("STOP_FIND_MY_DEVICE");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MyApplication.base, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                builder.setContentTitle("LanBridge查找设备")//设置通知栏标题
                        .setContentText("正在发出提示音和震动")
                        .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                        .setLargeIcon(BitmapFactory.decodeResource(MyApplication.base.getResources(), R.mipmap.ic_launcher_foreground, null))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)//设置通知小ICON
                        .setWhen(System.currentTimeMillis())
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setAutoCancel(false)
                        .setChannelId("channel")
                        .addAction(android.R.drawable.ic_notification_clear_all, "结束提醒", pendingIntent);
                builder.setContentIntent(pendingIntent);
                builder.setFullScreenIntent(pendingIntent, true); //设置通知栏点击意图
                Notification notification = builder.build();
                notificationManager.notify(101, notification);

                Thread find_my_device_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (find_my_device_running && System.currentTimeMillis() - t < find_my_device_duration * 1000) {
                                vibrator.vibrate(80);
                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            toolutils.cancel_notification(101);
                            mPlayer.stop();
                            mPlayer.release();
                            find_my_device_running = false;
                        }
                    }
                });
                find_my_device_thread.start();

                response.send("");
            }
        });

        server.get("/api/sync_folder_structure", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("{}");//不支持此功能，直接返回空结果
            }
        });

        server.post("/api/file_transmit", new HttpServerRequestCallback() {

            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                final MultipartFormDataBody body = (MultipartFormDataBody) request.getBody();
                String base_path = getApplicationContext().getExternalFilesDir(null).getPath() + File.separatorChar + "files_received_cache" + File.separatorChar;
                body.setMultipartCallback(new MultipartFormDataBody.MultipartCallback() {
                    @Override
                    public void onPart(Part part) {
                        if (!part.isFile() || !body.getField("type").equals("file")) return;
                        String root = body.getField("root");
                        String tag = body.getField("tag");

                        String path = base_path + tag + File.separatorChar + root + (root.isEmpty() ? "" : File.separatorChar) + part.getFilename();

                        try {
                            File file = new File(path);
                            File parent = file.getParentFile();
                            if (!parent.exists()) {
                                parent.mkdirs();
                            }
                            body.setDataCallback(new OutputStreamDataCallback(new FileOutputStream(file)));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                });

                request.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (body.getField("type").equals("dir")) {
                            String path = base_path + body.getField("tag") + File.separatorChar + body.getField("root");
                            new File(path).mkdirs();
                        }
                        response.send("");
                    }
                });
            }

        });

        server.get("/api/show_transmit_folder", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String tag = request.getQuery().getString("tag");

                String path = getApplicationContext().getExternalFilesDir(null).getPath() + File.separatorChar + "files_received_cache" + File.separatorChar + tag;
                toolutils.show_notification("文件接收完成", tag, String.format("请使用文件管理器前往%s查看", path), null, 99, false);

                response.send("");
            }
        });

        server.listen(8000);
    }

}