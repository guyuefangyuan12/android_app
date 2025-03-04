package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.myapplication.device.DeviceActivity;
import com.example.myapplication.modbus.ModbusTCPClient;
import com.example.myapplication.device.device_Control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ModbusTCPClient mtcp = ModbusTCPClient.getInstance();

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mtcp.connect("192.168.0.174", 4002, 1, MainActivity.this);
                    List<Integer> value = Arrays.asList(1, 2, 3);
                    //mtcp.writeMultipleRegisters(101, value);
                    List<Integer> data = mtcp.readHoldingRegisters(1, 101, 3);
                    Log.d("TCPTest", data.toString());
                } catch (ModbusTCPClient.ModbusException e) {
                    Log.d("TCPTest", e.getMessage());
                }
            }
        }).start();
    }

    public void onClickDevice(View view) {
        Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
        startActivity(intent);
    }

    public void testClick(View view) {
        Intent intent = new Intent(MainActivity.this, device_Control.class);
        startActivity(intent);
    }
}