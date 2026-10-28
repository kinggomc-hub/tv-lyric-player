package com.tv.lyricplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "AndroidUSB");
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.__back ? window.__back() : 'exit'", v -> {
            if (v == null || v.contains("exit")) finish();
        });
    }

    class Bridge {
        @JavascriptInterface
        public void exit() { runOnUiThread(() -> finish()); }

        @JavascriptInterface
        public String list(String path) {
            JSONArray a = new JSONArray();
            try {
                Map<String, File> m = new TreeMap<>();
                if (path.equals("/storage") || path.equals("/storage/")) {
                    File[] fs = new File("/storage").listFiles();
                    if (fs != null) for (File f : fs) m.put(f.getName(), f);
                    // 목록 권한이 막혀 있어도 USB 볼륨 루트는 이 방법으로 찾을 수 있음
                    for (File f : getExternalFilesDirs(null)) {
                        if (f == null) continue;
                        String p = f.getAbsolutePath();
                        int i = p.indexOf("/Android/data");
                        if (i > 0) { File r = new File(p.substring(0, i)); m.put(r.getName(), r); }
                    }
                } else {
                    File[] fs = new File(path).listFiles();
                    if (fs != null) for (File f : fs) m.put(f.getName(), f);
                }
                for (File f : m.values()) {
                    if (f.getName().startsWith(".")) continue;
                    JSONObject o = new JSONObject();
                    o.put("n", f.getName());
                    o.put("d", f.isDirectory());
                    a.put(o);
                }
            } catch (Exception e) { }
            return a.toString();
        }
    }
}
