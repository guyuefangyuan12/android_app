package com.example.myapplication.image;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class GCodeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private ImageView imageView;
    private TextView gcodeTextView;

    private Bitmap bitmap;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gcode);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.imageView);
        gcodeTextView = findViewById(R.id.gcodeTextView);

        bitmap = MainActivity.GetImageBitmap();
        gcodeTextView.setText("; Created using Image2GCode v1.0.0 https://github.com/damir3/Image2GCode\n" +
                "; Image Size: 100x100 mm\n" +
                "; Gamma: 1\n" +
                "; Mode: halftone\n" +
                "; Resolution: 1000x1000 px, 0.1x0.1 mm/px\n" +
                "; Laser Speed: 1000 mm/sec\n" +
                "; Travel Speed: 6000 mm/sec\n" +
                "; Power: [10, 255]\n" +
                "; Path: Raster\n" +
                ";\n" +
                "; Image engraving\n" +
                "M5\n" +
                "G1 X2 Y5 Z0 F6000\n" +
                "\n" +
                "G1 F1000\n" +
                "M3 I S0\n" +
                "G1 X2.1\n" +
                "G1 X2.2\n" +
                "G1 X2.3\n" +
                "G1 X2.4\n" +
                "G1 X2.5\n" +
                "G1 X2.6\n" +
                "G1 X2.7\n" +
                "G1 X2.8\n" +
                "G1 X2.9\n" +
                "G1 X3\n" +
                "G1 X3.1\n" +
                "G1 X3.2\n" +
                "G1 X3.3\n" +
                "G1 X3.4\n" +
                "G1 X3.5\n" +
                "G1 X3.6\n" +
                "G1 X3.7\n" +
                "G1 X3.8\n" +
                "G1 X3.9\n" +
                "G1 X4\n" +
                "G1 X4.1\n" +
                "G1 X4.2\n" +
                "G1 X4.3\n" +
                "G1 X4.4\n" +
                "G1 X4.5\n" +
                "G1 X4.6\n" +
                "G1 X4.7\n" +
                "G1 X4.8\n" +
                "G1 X4.9\n" +
                "G1 X5\n" +
                "M3 I S10\n" +
                "G1 X5.1\n" +
                "G1 X5.2\n" +
                "G1 X5.3\n" +
                "G1 X5.4\n" +
                "G1 X5.5\n" +
                "G1 X5.6\n" +
                "G1 X5.7\n" +
                "G1 X5.8\n" +
                "G1 X5.9\n" +
                "G1 X6\n" +
                "G1 X6.1\n" +
                "G1 X6.2\n" +
                "G1 X6.3\n" +
                "G1 X6.4\n" +
                "G1 X6.5\n" +
                "G1 X6.6\n" +
                "G1 X6.7\n" +
                "G1 X6.8\n" +
                "G1 X6.9\n" +
                "G1 X7\n" +
                "G1 X7.1\n" +
                "G1 X7.2\n" +
                "G1 X7.3\n" +
                "G1 X7.4\n" +
                "G1 X7.5\n" +
                "G1 X7.6\n" +
                "G1 X7.7\n" +
                "G1 X7.8\n" +
                "G1 X7.9\n" +
                "G1 X8\n" +
                "G1 X8.1\n" +
                "G1 X8.2\n" +
                "G1 X8.3\n" +
                "G1 X8.4\n" +
                "G1 X8.5\n" +
                "G1 X8.6\n" +
                "G1 X8.7\n" +
                "G1 X8.8\n" +
                "G1 X8.9\n" +
                "G1 X9\n" +
                "G1 X9.1\n" +
                "G1 X9.2\n" +
                "G1 X9.3\n" +
                "G1 X9.4\n" +
                "G1 X9.5\n" +
                "G1 X9.6\n" +
                "G1 X9.7\n" +
                "G1 X9.8\n" +
                "G1 X9.9\n" +
                "G1 X10\n" +
                "G1 X10.1\n" +
                "G1 X10.2\n" +
                "G1 X10.3\n" +
                "G1 X10.4\n" +
                "G1 X10.5\n" +
                "G1 X10.6\n" +
                "G1 X10.7\n" +
                "G1 X10.8\n" +
                "G1 X10.9\n" +
                "G1 X11\n" +
                "G1 X11.1\n" +
                "G1 X11.2\n" +
                "G1 X11.3\n" +
                "G1 X11.4\n" +
                "G1 X11.5\n" +
                "G1 X11.6\n" +
                "G1 X11.7\n" +
                "G1 X11.8\n" +
                "G1 X11.9\n" +
                "G1 X12\n" +
                "G1 X12.1\n" +
                "G1 X12.2\n" +
                "G1 X12.3\n" +
                "G1 X12.4\n" +
                "G1 X12.5\n" +
                "G1 X12.6\n" +
                "G1 X12.7\n" +
                "G1 X12.8\n" +
                "G1 X12.9\n" +
                "G1 X13\n" +
                "G1 X13.1\n" +
                "G1 X13.2\n" +
                "G1 X13.3\n" +
                "M3 I S255\n" +
                "G1 X13.4\n" +
                "M3 I S10\n" +
                "G1 X13.5\n" +
                "G1 X13.6\n" +
                "G1 X13.7\n" +
                "G1 X13.8\n" +
                "G1 X13.9\n" +
                "G1 X14\n" +
                "G1 X14.1\n" +
                "G1 X14.2\n" +
                "G1 X14.3\n" +
                "G1 X14.4\n" +
                "G1 X14.5\n" +
                "G1 X14.6\n" +
                "G1 X14.7\n" +
                "G1 X14.8\n" +
                "G1 X14.9\n" +
                "G1 X15\n" +
                "G1 X15.1\n" +
                "G1 X15.2\n" +
                "G1 X15.3\n" +
                "G1 X15.4\n" +
                "G1 X15.5\n" +
                "G1 X15.6\n" +
                "G1 X15.7\n" +
                "G1 X15.8\n" +
                "G1 X15.9\n" +
                "G1 X16\n" +
                "G1 X16.1\n" +
                "G1 X16.2\n" +
                "G1 X16.3\n" +
                "G1 X16.4\n" +
                "G1 X16.5\n" +
                "G1 X16.6\n" +
                "G1 X16.7\n" +
                "G1 X16.8\n" +
                "G1 X16.9\n" +
                "G1 X17\n" +
                "G1 X17.1\n" +
                "G1 X17.2\n" +
                "G1 X17.3\n" +
                "G1 X17.4\n" +
                "G1 X17.5\n" +
                "G1 X17.6\n" +
                "G1 X17.7\n" +
                "G1 X17.8\n" +
                "G1 X17.9\n" +
                "G1 X18\n" +
                "G1 X18.1\n" +
                "G1 X18.2\n" +
                "G1 X18.3\n" +
                "G1 X18.4\n" +
                "G1 X18.5\n" +
                "G1 X18.6\n" +
                "G1 X18.7\n" +
                "G1 X18.8\n" +
                "M3 I S255\n" +
                "G1 X18.9\n" +
                "M3 I S10");

        imageView.setImageBitmap(bitmap);
    }


}