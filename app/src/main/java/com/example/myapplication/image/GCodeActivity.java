package com.example.myapplication.image;

import android.annotation.SuppressLint;
import android.app.blob.BlobHandle;
import android.app.blob.BlobStoreManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Blob;

public class GCodeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private ImageView imageView;
    private TextView gcodeTextView;
    private ScrollView scrollView;
    private WebView webView;
    private Bitmap bitmap;
    private Uri imageUri;
    private String imageUriString;
    private String line;
    private boolean isLoading = false;
    private StringBuilder fileContentBuilder = new StringBuilder();
    private BufferedReader reader;

    @SuppressLint({"SetTextI18n", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File file = new File(getFilesDir(), "example.txt");
        StringBuffer sb = new StringBuffer();
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
        scrollView = findViewById(R.id.scrollView);

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            imageView.setImageBitmap(bitmap);


        } catch (IOException e) {
            e.printStackTrace();
        }

        // WEBVIEW 设置
        webView.setWebViewClient(new WebViewClient());
        WebSettings webViewSetting = webView.getSettings();
        // 启用 JavaScript
        webViewSetting.setJavaScriptEnabled(true);
        //设置缓存模式
        webViewSetting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // 开启DOM storage API 功能
        webViewSetting.setDomStorageEnabled(true);
        // 开启database storage API功能
        webViewSetting.setDatabaseEnabled(true);
        // 开启DOM缓存，开启LocalStorage存储（html5的本地存储方式）
        webViewSetting.setAllowUniversalAccessFromFileURLs(true);
        webViewSetting.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        webViewSetting.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true

        webViewSetting.setUseWideViewPort(true); // 支持内容自适应宽度
        webViewSetting.setLoadWithOverviewMode(true); // 页面内容缩放适应屏幕
        webViewSetting.setSupportZoom(true); // 支持缩放
        webViewSetting.setBuiltInZoomControls(true); // 内置缩放控件
        webViewSetting.setDisplayZoomControls(false); // 隐藏缩放控制
        //webViewSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

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
                while(webView.zoomIn());
            }
        });
        imageView.setImageBitmap(bitmap);
        //gcodeTextView.setText(":");

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void saveGCode(String GCode) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    // 创建一个动态变化的字符串
                    writer.write(GCode);

                    // 输出到控制台，便于查看写入的内容
                    Log.e("TEST","Written: " + GCode);

                    // 每次写入后等待一秒
                    //Thread.sleep(1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @android.webkit.JavascriptInterface
            public void showGCode() throws IOException{

                FileInputStream fis = openFileInput("example.txt");
                reader = new BufferedReader(new InputStreamReader(fis));

                // 动态加载文本内容
                loadMoreContent();

                // 监听 ScrollView 滚动
                scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        // 判断是否滚动到 ScrollView 底部
                        if (scrollView.getChildAt(0).getBottom() <= (scrollView.getHeight() + scrollView.getScrollY())) {
                            // 滚动到底部，加载更多内容
                            if (!isLoading) {
                                loadMoreContent();
                            }
                        }
                    }
                });

            }

            @android.webkit.JavascriptInterface
            public void receiveBlobData(String data) {
                // 这里接收到的是 Blob 数据（通常为 Data URL）
                handleBlobData(data);
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
    private void handleBlobData(String data) {
        // 在这里可以处理接收到的 Blob 数据（例如，保存到文件、数据库等）
        gcodeTextView.setText(data);
    }
    // 加载更多内容
    private void loadMoreContent() {
        isLoading = true;

        // 模拟延时加载
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    int batchSize = 100; // 每次加载100行
                    int lineCount = 0;

                    while ((line = reader.readLine()) != null && lineCount < batchSize) {
                        fileContentBuilder.append(line).append("\n");
                        lineCount++;
                    }

                    // 使用 runOnUiThread 更新 UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 更新 TextView
                            gcodeTextView.setText(fileContentBuilder.toString());

                            // 如果没有更多内容，就表示加载完毕
                            if (line == null) {
                                Toast.makeText(GCodeActivity.this, "No more content", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }

                isLoading = false;
            }
        }, 500); // 模拟延时
    }

}