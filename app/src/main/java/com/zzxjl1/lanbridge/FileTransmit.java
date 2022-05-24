package com.zzxjl1.lanbridge;


import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class FileTransmit extends AppCompatActivity {
    ProgressDialog progressDialog;
    private WebView webView;
    private FloatingActionButton refresh_btn;
    ArrayList<Uri> uris;
    private Intent intent;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transmit);

        intent = getIntent();
        action = intent.getAction();

        webView = findViewById(R.id.webView);
        refresh_btn = findViewById(R.id.refresh);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.evaluateJavascript("update_all_lan_clients()", null);
                Toast.makeText(FileTransmit.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }

        });
        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void send_file(String ip) {
                System.out.println(ip);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (action == null) return;
                        switch (action) {
                            case Intent.ACTION_SEND:
                                Bundle bundle = intent.getExtras();
                                Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                                uris = new ArrayList<>();
                                uris.add(uri);
                                start_file_transmit(uris, ip);
                                break;
                            case Intent.ACTION_SEND_MULTIPLE:
                                uris = intent.getParcelableArrayListExtra(intent.EXTRA_STREAM);
                                start_file_transmit(uris, ip);
                                break;
                        }
                    }
                });

            }

            @JavascriptInterface
            public String get_all_lan_clients() {
                return new JSONObject(connections.data).toString();
            }

        }, "eel");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webView.loadUrl("file:///android_asset/ui/select_device.html");
        webView.setWebContentsDebuggingEnabled(true);

    }


    void start_file_transmit(ArrayList<Uri> uris, String ip) {
        progressDialog = new ProgressDialog(FileTransmit.this);
        progressDialog.setTitle(String.format("正在向%s发送文件", ip));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String tag = String.format("%s from %s", new SimpleDateFormat("yyyy_MM_dd HH_mm_ss").format(Calendar.getInstance().getTime()), toolutils.get_hostname());
                try {
                    toolutils.emptyDir(new File(MyApplication.base.getFilesDir().getPath() + File.separatorChar + "get_file_cache"));
                    for (int i = 0; i < uris.size(); i++) {
                        Uri uri = uris.get(i);
                        File t = toolutils.getFile(MyApplication.base, uri);
                        int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.setTitle(String.format("正在向%s发送文件\n(第%d个,共%d个)", ip, finalI + 1, uris.size()));
                            }
                        });
                        send(t, ip, tag);
                        t.delete();
                    }
                    String url = String.format("http://%s:%d/api/show_transmit_folder?tag=%s", ip, 8000, URLEncoder.encode(tag));
                    AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(url), null);
                    progressDialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FileTransmit.this, "文件传输完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();


    }

    void send(File file, String ip, String tag) throws ExecutionException, InterruptedException {

        Ion.with(FileTransmit.this)
                .load(String.format("http://%s:%d/api/file_transmit", ip, 8000))
                .uploadProgressHandler(new ProgressCallback() {
                    @Override
                    public void onProgress(long downloaded, long total) {
                        int val = (int) ((downloaded / (float) total) * 100);
                        Log.e("PROGRESS:", String.valueOf(val));
                        progressDialog.setProgress(val);
                    }
                })
                .setMultipartParameter("type", "file")
                .setMultipartParameter("tag", tag)
                .setMultipartParameter("root", "")
                .setMultipartFile("file", "text/plain", file)
                .asString()
                .get();
    }


}

