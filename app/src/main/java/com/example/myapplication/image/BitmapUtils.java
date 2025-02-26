package com.example.myapplication.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class BitmapUtils {

    // 将 String 转换为 Bitmap
    public static Bitmap stringToBitmap(String encodedString) {
        byte[] decodedByte = Base64.decode(encodedString, Base64.NO_WRAP);  // 解码 Base64 字符串为字节数组
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);  // 从字节数组解码为 Bitmap
    }
    public static String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);  // 使用 PNG 格式，无压缩
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);  // 编码为 Base64 字符串
    }
}
