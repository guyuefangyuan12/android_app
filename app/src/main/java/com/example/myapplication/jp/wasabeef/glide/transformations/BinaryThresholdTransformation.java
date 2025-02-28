package com.example.myapplication.jp.wasabeef.glide.transformations;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.security.MessageDigest;

/**
 * 自定义 Glide 转换器，将图像转换为黑白二值化效果。
 */
public class BinaryThresholdTransformation extends BitmapTransformation {

  private static final int VERSION = 1; // 该转换器的版本
  private static final String ID =
          "jp.wasabeef.glide.transformations.BinaryThresholdTransformation." + VERSION; // 转换器的唯一标识符

  private final int threshold; // 二值化的阈值

  /**
   * 构造函数，接受一个阈值参数
   *
   * @param threshold 黑白二值化的阈值，通常范围在0-255之间
   */
  public BinaryThresholdTransformation(int threshold) {
    this.threshold = threshold;
  }
  public BinaryThresholdTransformation() {
    this.threshold = 127;
  }

  /**
   * 将输入的 Bitmap 转换为黑白二值化图像。
   *
   * @param context     上下文
   * @param pool        Bitmap 池，用于回收位图
   * @param toTransform 输入的 Bitmap 图像
   * @param outWidth    输出图像的宽度
   * @param outHeight   输出图像的高度
   * @return 转换后的黑白二值化 Bitmap 图像
   */
  @Override
  protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
                             @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    int width = toTransform.getWidth();
    int height = toTransform.getHeight();

    // 获取输入图像的配置，如果没有则使用 ARGB_8888 配置
    Bitmap.Config config = toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.ARGB_8888;

    // 从 Bitmap 池中获取具有所需宽度、高度和配置的 Bitmap
    Bitmap bitmap = pool.get(width, height, config);

    // 创建一个 Canvas 用于绘制转换后的 Bitmap
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint();

    // 遍历图像的每个像素，进行二值化处理
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // 获取当前像素的颜色
        int pixelColor = toTransform.getPixel(x, y);

        // 获取该像素的 RGB 分量
        int r = Color.red(pixelColor);
        int g = Color.green(pixelColor);
        int b = Color.blue(pixelColor);

        // 计算该像素的灰度值，使用加权平均法：0.299 * R + 0.587 * G + 0.114 * B
        int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        // 根据阈值判断该像素是变为白色还是黑色
        if (gray >= threshold) {
          paint.setColor(Color.WHITE); // 白色
        } else {
          paint.setColor(Color.BLACK); // 黑色
        }

        // 在 Canvas 上绘制该像素的颜色
        canvas.drawPoint(x, y, paint);
      }
    }

    // 返回转换后的二值化 Bitmap
    return bitmap;
  }

  /**
   * 返回该转换器的字符串表示。
   *
   * @return BinaryThresholdTransformation 的字符串表示
   */
  @Override
  public String toString() {
    return "BinaryThresholdTransformation(threshold=" + threshold + ")";
  }

  /**
   * 判断两个 BinaryThresholdTransformation 对象是否相等。
   *
   * @param o 要比较的对象
   * @return 如果两个对象是相同的转换器，则返回 true
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BinaryThresholdTransformation that = (BinaryThresholdTransformation) o;
    return threshold == that.threshold;
  }

  /**
   * 返回该转换器的哈希值。
   *
   * @return 转换器的哈希值
   */
  @Override
  public int hashCode() {
    return ID.hashCode() + threshold;
  }

  /**
   * 更新该转换器的磁盘缓存密钥。
   *
   * @param messageDigest 用于更新缓存密钥的 MessageDigest
   */
  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    // 更新缓存密钥，使用该转换器的唯一 ID 和阈值
    messageDigest.update((ID + threshold).getBytes(CHARSET));
  }
}
