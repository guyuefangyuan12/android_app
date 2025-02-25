package com.example.myapplication.device;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DeviceActivity extends AppCompatActivity implements UdpReceiver.OnDeviceReceivedListener {
    private static final String TAG = "UdpListener";
    private static final long DEVICE_EXPIRATION_MS = 10 * 1000; // 10秒超时
    private Set<Device> deviceSet;
    private UdpReceiver udpReceiver;
    private DeviceTableAdapter deviceTableAdapter;
    private RecyclerView deviceTable;
    private ExecutorService executorService;
    private Handler expirationHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceSet = new CopyOnWriteArraySet<>();

        deviceTable = findViewById(R.id.recyclerViewDevice);
        deviceTable.setLayoutManager(new LinearLayoutManager(this));

        DeviceTableAdapter.OnDeviceClickListener ondeviceClickListener = new DeviceTableAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {

            }
        };

        deviceTableAdapter = new DeviceTableAdapter(new CopyOnWriteArrayList<>(), deviceSet, ondeviceClickListener);
        deviceTable.setAdapter(deviceTableAdapter);
        //添加Android自带的分割线
        deviceTable.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        executorService = Executors.newSingleThreadExecutor();

        udpReceiver = new UdpReceiver(4001, new Handler(Looper.getMainLooper()), this);
        udpReceiver.startReceiving();
    }

    @Override
    public void onDeviceReceived(Device device) {
        if (!deviceSet.contains(device)) {
            deviceTableAdapter.addData(device);
        } else {
            // 如果已存在，更新时间戳
            device.updateTimestamp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expirationHandler.removeCallbacksAndMessages(null);
        udpReceiver.closeSocket();
        udpReceiver.shutdownExecutor();
        executorService.shutdown();
    }

    // 定期清理过期设备
    private void startExpirationCheck() {
        expirationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cleanExpiredDevices();
                expirationHandler.postDelayed(this, DEVICE_EXPIRATION_MS);
            }
        }, DEVICE_EXPIRATION_MS);
    }

    private void cleanExpiredDevices() {
        new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Device> iterator = deviceSet.iterator();
            while (iterator.hasNext()) {
                Device device = iterator.next();
                if (currentTime - device.getLastUpdated() > DEVICE_EXPIRATION_MS) {
                    iterator.remove();
                    deviceTableAdapter.removeDevice(device);
                }
            }
        }).start();
    }
}