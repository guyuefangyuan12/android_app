package com.example.myapplication.device;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.modbus.ModbusTCPClient;

import java.util.ArrayList;
import java.util.List;

public class device_Control extends AppCompatActivity {
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();
    private static final String TAG = "devicecontrol";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_control);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public static short[] convertIntToShortBigEndian(int[] intArray) {
        if (intArray == null) {
            throw new IllegalArgumentException("输入数组不能为null");
        }
        short[] shortArray = new short[intArray.length * 2];
        for (int i = 0; i < intArray.length; i++) {
            int value = intArray[i];
            // 提取高16位（前2字节）
            short high = (short) ((value >> 16) & 0xFFFF);
            // 提取低16位（后2字节）
            short low = (short) (value & 0xFFFF);
            // 按大端顺序存储：高16位在前，低16位在后
            shortArray[i * 2] = high;
            shortArray[i * 2 + 1] = low;
        }
        return shortArray;
    }

    public static List<Integer> convertIntToShortLittleEndian(int[] intArray) {
        if (intArray == null) {
            throw new IllegalArgumentException("输入数组不能为null");
        }
        List<Integer> Array = new ArrayList<>();
        for (int i = 0; i < intArray.length; i++) {
            int value = intArray[i];
            // 提取低16位（后2字节）
            int low = (value & 0xFFFF);
            // 提取高16位（前2字节）
            int high = ((value >> 16) & 0xFFFF);
            // 按小端顺序存储：低16位在前，高16位在后
            Array.add(low);
            Array.add(high);
        }
        return Array;
    }

    public void testClick(View view) {
        int[] value = {4, 1, 0};
        List<Integer> values = convertIntToShortLittleEndian(value);

        ModbusTCPClient.ModbusCallback<int[]> modbusCallback = new ModbusTCPClient.ModbusCallback<int[]>() {
            @Override
            public void onSuccess(int[] result) {
                Log.d(TAG, "fasongchengg");
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "fasongshibai");
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //mtcp.FileTransPort(101, "abc.enc", device_Control.this);
                    //mtcp.writeReg(101,values);
                    List<Integer> data = mtcp.readReg(1, 1000, 6);
                    Log.d("devicecontrol", data.toString());
                } catch (ModbusTCPClient.ModbusException e) {
                    Log.d("devicecontrol", e.getMessage());
                }
            }
        }).start();

    }
}