package com.example.accountvault;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView wv;
    private ValueCallback<Uri[]> mUpload;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);

        wv.setWebViewClient(new WebViewClient());
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams params) {
                mUpload = filePathCallback;
                try {
                    Intent i = params.createIntent();
                    startActivityForResult(Intent.createChooser(i, "选择备份文件"), 1001);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        });

        wv.addJavascriptInterface(new AppBridge(), "AndroidSave");
        wv.loadUrl("file:///android_asset/index.html");
        setContentView(wv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && mUpload != null) {
            Uri u = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
            mUpload.onReceiveValue(u != null ? new Uri[]{u} : null);
            mUpload = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) wv.goBack();
        else super.onBackPressed();
    }

    public class AppBridge {
        @JavascriptInterface
        public void saveFile(String filename, String text, String mime) {
            try {
                byte[] data = text.getBytes("UTF-8");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE,
                            (mime == null || mime.isEmpty()) ? "application/json" : mime);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        os.write(data);
                        os.flush();
                        os.close();
                        toast("已导出到「下载」：" + filename);
                        return;
                    }
                }
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "账号保险库");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, filename);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data);
                fo.flush();
                fo.close();
                toast("已导出到：" + f.getAbsolutePath());
            } catch (Exception e) {
                toast("导出失败：" + e.getMessage());
            }
        }

        private void toast(final String msg) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
