package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.device.DeviceActivity;
import com.example.myapplication.image.GCodeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private ImageView imageView;

    private static Uri imageUri;

    public static Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.imageView);
    }

    public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
        startActivity(intent);
    }

    public void selectImage(View view) {
        Intent intent1 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent1, PICK_IMAGE);
//        Intent intent = new Intent(MainActivity.this, GCodeActivity.class);
//        intent.putExtra("bitmap", BitmapUtils.bitmapToString(bitmap)); // 将图片 bitmap 转为字符串传递
        //startActivity(intent);
    }

    public void captureImage(View view){
        Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent1, CAPTURE_IMAGE);
//        Intent intent = new Intent(MainActivity.this, GCodeActivity.class);
//        intent.putExtra("bitmap", BitmapUtils.bitmapToString(bitmap)); // 将图片 bitmap 转为字符串传递
        //startActivity(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null){
            imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Intent intent = new Intent(MainActivity.this, GCodeActivity.class);
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);

                imageView.setImageBitmap(bitmap);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        else if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK && data != null){
            bitmap = (Bitmap) data.getExtras().get("data");
            // 将 Bitmap 保存到临时文件并获取其 URI
            try {
                File tempFile = createImageFile(); // 创建临时文件
                FileOutputStream out = new FileOutputStream(tempFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                imageUri = Uri.fromFile(tempFile);

                // 传递 URI 给下一个 Activity
                Intent intent = new Intent(MainActivity.this, GCodeActivity.class);
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);

                imageView.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 创建临时图片文件
    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );
        return image;
    }

    public static Bitmap GetImageBitmap()
    {
        return bitmap;
    }

    public static Uri GetImageUri()
    {
        return imageUri;
    }
}