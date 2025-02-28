package com.example.myapplication.image;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.RequestOptions;
import com.example.myapplication.R;
import com.example.myapplication.jp.wasabeef.glide.transformations.BinaryThresholdTransformation;

import jp.wasabeef.glide.transformations.GrayscaleTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class ImageEditActivity extends AppCompatActivity {

    private ImageView imageView;
    private Uri imageUri;


    private int rotationAngle = 0;
    //private CircularProgressDrawable progressDrawable = new CircularProgressDrawable(this);

    @SuppressLint({"SetTextI18n", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageedit);

        // 初始化视图
        imageView = findViewById(R.id.imageView);
        Button btnRotate = findViewById(R.id.btnRotate);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button btnFilter = findViewById(R.id.btnFilter);
        Button btnBinary = findViewById(R.id.btnBinary);

        // 使用Glide加载图片
        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        loadImageWithGlide();

        // 旋转按钮点击
        btnRotate.setOnClickListener(v -> {
            rotationAngle += 90;
            Glide.with(this)
                    .load(imageUri)
                    .apply(RequestOptions.bitmapTransform(new Rotate(rotationAngle)))
                    .into(imageView);
        });

        // 裁剪按钮点击（示例：圆角裁剪）
        btnCrop.setOnClickListener(v -> Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.bitmapTransform(
                        new RoundedCornersTransformation(50, 0)))
                .into(imageView));

        // 滤镜按钮点击（示例：灰度滤镜）
        btnFilter.setOnClickListener(v -> Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.bitmapTransform(
                        new GrayscaleTransformation()))
                .into(imageView));

        // 黑白按钮
        btnBinary.setOnClickListener(v -> Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.bitmapTransform(
                        new BinaryThresholdTransformation()))
                .into(imageView));
    }

    private void loadImageWithGlide() {
        Glide.with(this)
                .load(imageUri)
                .apply(new RequestOptions()
                        //TODO ic_loading.xml 自定义loading动画
                        .placeholder(new CircularProgressDrawable(this))
                        .error(R.drawable.ic_error))
                .into(imageView);
    }



    // 添加更多编辑功能...
}