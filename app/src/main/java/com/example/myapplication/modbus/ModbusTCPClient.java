package com.example.myapplication.modbus;


import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import android.util.Log;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


public class ModbusTCPClient {
    private static final ModbusTCPClient INSTANCE = new ModbusTCPClient();
    private static final String TAG = "ModbusTCPClient";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger transactionId = new AtomicInteger(0);
    private final Object lock = new Object();
    private int unitId = 0;
    private Socket socket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private ModbusTCPClient() {

    }

    public static ModbusTCPClient getInstance() {
        return INSTANCE;
    }

    public void connect(String host, int port, int unitId, Context context) throws ModbusException {
        connect(5000, host, port, unitId, context);
    }

    public void connect(int timeout, String host, int port, int unitId, Context context) throws ModbusException {
        this.unitId = unitId;
        synchronized (lock) {
            try {
                if (isConnected.get()) {
                    if (socket.getInetAddress().getHostAddress() == host && socket.getPort() == port) {
                        return;
                    }
                    disconnect();
                }
                socket = new Socket(host, port);
                socket.setSoTimeout(timeout);
                inputStream = new BufferedInputStream(socket.getInputStream());
                outputStream = new BufferedOutputStream(socket.getOutputStream());
                isConnected.set(true);
            } catch (IOException e) {
                disconnect();
                throw new ModbusException("Connection failed: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        synchronized (lock) {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
            isConnected.set(false);
        }
    }

    private void validateConnection() throws ModbusException {
        if (!isConnected.get()) {
            throw new ModbusException("Not connected");
        }
    }

    public static byte[] readBytes(BufferedInputStream in, int expectedSize) throws IOException {
        byte[] data = new byte[expectedSize];
        int totalRead = 0;

        while (totalRead < expectedSize) {
            int remaining = expectedSize - totalRead;
            // 从流中读取数据到数组的指定位置
            int bytesRead = in.read(data, totalRead, remaining);

            if (bytesRead == -1) {
                // 流已结束，但未读取足够数据
                throw new IOException("Unexpected end of stream. Expected " + expectedSize + " bytes, but got " + totalRead);
            }
            totalRead += bytesRead;
        }
        return data;
    }

    private List<Integer> Response(int expectedFunction) throws IOException, ModbusException {
        byte[] header = readBytes(inputStream, ModBuscode.MbapFrameLen);
        List<Integer> data = new ArrayList<>();
        try {
            int[] mbapHeader = ModBuscode.parseMbapFrame(header);
            byte[] pdu = readBytes(inputStream, mbapHeader[2] - 1);
            switch (expectedFunction) {
                case ModBuscode.ReadFunCode:
                    data = ModBuscode.decodeReadHoldingRegisters(pdu, mbapHeader[2] - 1);
                    break;
                case ModBuscode.WriteFunCode:
                    data = ModBuscode.decodeWriteMultipleRegisters(pdu, mbapHeader[2] - 1);
                    break;
                case ModBuscode.FileFunCode:
                    data = ModBuscode.decodeFileTransport(pdu, mbapHeader[2] - 1);
                    break;
            }
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        return data;
    }

    public List<Integer> readHoldingRegisters(int unitId, int startAddress, int quantity) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeReadHoldingRegisters(transactionId.incrementAndGet(), unitId, startAddress, quantity);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (lock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                List<Integer> response = Response(ModBuscode.ReadFunCode);
                return response;
            } catch (IOException e) {
                throw new ModbusException("Communication error: " + e.getMessage());
            }

        }
    }

    private void validateWriteResponse(List<Integer> data, int startAddress, int quantity)
            throws ModbusException {
        if (data.get(0) != startAddress || data.get(1) != quantity) {
            throw new ModbusException("Write validation failed");
        }
    }

    public void writeMultipleRegisters(final int startAddr, List<Integer> values) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeWriteMultipleRegisters(transactionId.incrementAndGet(), unitId, startAddr, values);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (lock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                List<Integer> response = Response(ModBuscode.WriteFunCode);
                validateWriteResponse(response, startAddr, values.size());
            } catch (IOException e) {
                throw new ModbusException("Communication error: " + e.getMessage());
            }
        }
    }

    private void validateFileTransportResponse(List<Integer> data, int fileAddress, int quantity)
            throws ModbusException {
        if (data.get(0) != fileAddress || data.get(1) != quantity) {
            throw new ModbusException("FileTransport validation failed");
        }
    }

    public void FileTransportByte(final int fileAddr, byte[] byteData) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeFileTransport(transactionId.incrementAndGet(), unitId, fileAddr, byteData);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (lock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                List<Integer> response = Response(ModBuscode.FileFunCode);
                validateFileTransportResponse(response, fileAddr, byteData.length);
            } catch (IOException e) {
                throw new ModbusException("Communication error: " + e.getMessage());
            }
        }
    }


    public static List<Integer> byteToint(List<Integer> value) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            int val = value.get(i);
            // 提取高16位（前2字节）
            values.add(val & 0xFF);
            values.add(val >> 16 & 0xFF);
            // 提取低16位（后2字节）
        }
        return values;
    }

    public void FileTransPort(final int fileAddr, String filename, Context context) throws ModbusException {
        int bufferSize = 1024;
        //File file = new File(context.getFilesDir(), "largefile.bin");
        List<Integer> value = new ArrayList<>();
        value.add(7);
        for (int i = 0; i < filename.length(); i++) {
            value.add((int) filename.charAt(i));
        }
        value.add((int) '&');
        try (InputStream fis = context.getAssets().open("abc.enc")) {
            String md5String = md5Hex(fis);
            for (int i = 0; i < md5String.length(); i++) {
                value.add((int) md5String.charAt(i));
            }
            List<Integer> values = byteToint(value);
            writeMultipleRegisters(101, values);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            InputStream fis1 = context.getAssets().open("abc.enc");
            while ((bytesRead = fis1.read(buffer)) != -1) {
                // 判断是否为最后一次读取（可能不足缓冲区大小）
                byte[] aligendBytes = new byte[bytesRead];
                for (int i = 0; i < bytesRead; i++) {
                    aligendBytes[i] = buffer[i];
                }
                //aligendBytes = byteToint(aligendBytes);
                FileTransportByte(fileAddr, aligendBytes);
            }
            value.clear();
            values.clear();
            value.add(8);
            for (int i = 0; i < value.size(); i++) {
                int val = value.get(i);
                // 提取高16位（前2字节）
                values.add(val & 0xFF);
                values.add(val >> 16 & 0xFF);
                // 提取低16位（后2字节）
            }
            writeMultipleRegisters(101, values);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onConnected(Context context) {
        Toast toast = Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void onConnectionFailed(Context context) {
        Toast toast = Toast.makeText(context, "连接失败", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void onConnectionFailed(String error) {
        // Notify UI connection failed
    }

    private void onWriteSuccess() {
        // Notify UI write success
    }

    private void onWriteFailed(String error) {
        // Notify UI write failed
    }

    public interface ModbusCallback<T> {
        void onSuccess(T result);

        void onError(String error);
    }

    public static class ModbusException extends Exception {
        public ModbusException(String message) {
            super(message);
        }

        public static String getExceptionMessage(int code) {
            switch (code) {
                case 0x01:
                    return "Illegal Function";
                case 0x02:
                    return "Illegal Data Address";
                case 0x03:
                    return "Illegal Data Value";
                case 0x04:
                    return "Server Device Failure";
                default:
                    return "Unknown error (" + code + ")";
            }
        }
    }
}
