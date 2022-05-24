package com.zzxjl1.lanbridge;


import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class toolutils {

    public static String get_cpu_name() {
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            br.close();
            String[] array = text.split(":\\s+", 2);
            if (array.length >= 2) {
                return array[1];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String get_ip() {
        WifiManager wifi = (WifiManager) MyApplication.base.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
    }

    public static String get_hostname() {
        String hostName;
        try {
            InetAddress netHost = InetAddress.getByName(get_ip());
            hostName = netHost.getHostName();
        } catch (UnknownHostException ex) {
            hostName = "";
        }

        return hostName;
    }

    static HashMap<Object, Object> get_sys_info() {
        return new HashMap<Object, Object>() {{
            put("platform", String.format("%s %s(%s)", Build.MANUFACTURER, Build.MODEL, Build.BOARD));
            put("hostname", get_hostname());
            //put("architecture", Build.HARDWARE);
            put("processor", get_cpu_name());
            put("system", "Android");
            put("mac", Build.SERIAL);
        }};

    }


    public static File getFile(Context context, Uri uri) throws IOException {
        File destinationFilename = new File(context.getFilesDir().getPath() + File.separatorChar + "get_file_cache" + File.separatorChar + queryName(context, uri));
        File parent = destinationFilename.getParentFile();
        if (!parent.exists()) parent.mkdir();
        try (InputStream ins = context.getContentResolver().openInputStream(uri)) {
            createFileFromStream(ins, destinationFilename);
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
        return destinationFilename;
    }

    public static void createFileFromStream(InputStream ins, File destination) {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = ins.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static String queryName(Context context, Uri uri) {
        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    public static void emptyDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                emptyDir(new File(dir, children[i]));
            }
        } else {
            dir.delete();
        }
    }

    public static void show_notification(String title, String body, int id) {
        Intent notificationIntent = new Intent(MyApplication.base, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(MyApplication.base, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        show_notification(title, body,null,pendingIntent, id,true);
    }
    public static void show_notification(String title, String body, String bigtext, PendingIntent notificationIntent, int id,boolean ongoing) {
        NotificationManager notificationManager = (NotificationManager) MyApplication.base.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel", "notify", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.base);

        builder.setContentTitle(title)//设置通知栏标题
                .setContentText(body)
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                .setLargeIcon(BitmapFactory.decodeResource(MyApplication.base.getResources(), R.mipmap.ic_launcher_foreground, null))
                .setSmallIcon(R.drawable.ic_launcher_foreground)//设置通知小ICON
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setChannelId("channel")
                .setOngoing(ongoing);

        if (bigtext!=null&&!bigtext.isEmpty()) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigtext));
        }
        if (notificationIntent!=null){
            builder.setContentIntent(notificationIntent); //设置通知栏点击意图
        }
        Notification notification = builder.build();
        notificationManager.notify(id, notification);

    }

    static void cancel_notification(int id) {
        NotificationManager notificationManager = (NotificationManager) MyApplication.base.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}
