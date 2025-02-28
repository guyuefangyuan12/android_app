package com.example.myapplication.image;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.io.FileOutputStream;
import java.io.IOException;

public class GCodeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private ImageView imageView;
    private TextView gcodeTextView;
    private WebView webView;
    private Bitmap bitmap;
    private Uri imageUri;
    private String imageUriString;

    private StringBuilder fileContentBuilder;  // 用来合并数据块

    @SuppressLint({"SetTextI18n", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StringBuilder sb = new StringBuilder();
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gcode);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 获取传递过来的 imageUri 字符串
        imageUriString = getIntent().getStringExtra("imageUri");
        imageUri = Uri.parse(imageUriString);

        imageView = findViewById(R.id.imageView);
        webView = findViewById(R.id.webview);
        gcodeTextView = findViewById(R.id.gcodeTextView);
        gcodeTextView.setText("TTTEST");
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            imageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // WEBVIEW 设置
        WebSettings webviewSetting = webView.getSettings();
        // 启用 JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        //设置缓存模式
        webviewSetting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // 开启DOM storage API 功能
        webviewSetting.setDomStorageEnabled(true);
        // 开启database storage API功能
        webviewSetting.setDatabaseEnabled(true);
        // 开启DOM缓存，开启LocalStorage存储（html5的本地存储方式）
        webviewSetting.setAllowUniversalAccessFromFileURLs(true);
        webviewSetting.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webviewSetting.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true

        // 加载本地 HTML 文件
        webView.loadUrl("file:///android_asset/Image2GCode.html");
        // 等待 WebView 加载完成后，注入 URI 到 JavaScript
        //webView.setWebViewClient(new WebViewClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // 使用 evaluateJavascript 注入 URI 作为图片的源
                String uriString = imageUri.toString();
                webView.evaluateJavascript("loadImage('" + uriString + "');", null);
            }
        });
        imageView.setImageBitmap(bitmap);
        gcodeTextView.setText(":");

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void saveGCode(String gcode) {
                sb.append(gcode);
            }

            @android.webkit.JavascriptInterface
            public void showGCode() {
                try {
                    gcodeTextView.setText(sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Android");
    }
    // 保存文件
    private void saveFile(String data) {
        try {
            FileOutputStream fos = openFileOutput("largeDataFile.txt", MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();

            Toast.makeText(this, "Data saved successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }


}