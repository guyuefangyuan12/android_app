package com.example.myapplication.modbus;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import android.util.Log;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.widget.Toast;


public class ModbusTCPClient {
    private static final ModbusTCPClient INSTANCE = new ModbusTCPClient();
    private static final String TAG = "ModbusTCPClient";
    //private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger transactionId = new AtomicInteger(0);
    private final Object lock = new Object();
    private int unitId = 0;
    private Socket socket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    //private Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 创建一个ModbusTCPClient实例
     */
    private ModbusTCPClient() {

    }

    /**
     * 获取ModbusTCPClient的实例
     *
     * @return ModbusTCPClient的实例
     */
    public static ModbusTCPClient getInstance() {
        return INSTANCE;
    }

    /**
     * 连接Modbus TCP设备
     *
     * @param host    Modbus TCP设备的IP地址
     * @param port    Modbus TCP设备的端口号
     * @param unitId  Modbus TCP设备的单元ID
     * @param context Context对象
     * @throws ModbusException 连接失败时抛出
     */
    public void connect(String host, int port, int unitId, Context context) throws ModbusException {
        connect(5000, host, port, unitId, context);
    }

    /**
     * 连接Modbus TCP设备
     *
     * @param timeout 连接超时时间（毫秒）
     * @param host    Modbus TCP设备的IP地址
     * @param port    Modbus TCP设备的端口号
     * @param unitId  Modbus TCP设备的单元ID
     * @param context Context对象
     * @throws ModbusException 连接失败时抛出
     */
    public void connect(int timeout, String host, int port, int unitId, Context context) throws ModbusException {
        this.unitId = unitId;
        synchronized (lock) {
            try {
                if (isConnected.get()) {
                    // 如果已经连接了，检查是否是同一个设备
                    if (Objects.equals(socket.getInetAddress().getHostAddress(), host) && socket.getPort() == port) {
                        return;
                    }
                    // 如果不是同一个设备，断开当前连接
                    disconnect();
                }
                // 创建一个新的Socket对象
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

    /**
     * 断开Modbus TCP连接
     */
    public void disconnect() {
        synchronized (lock) {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
            // 将连接状态设置为false
            isConnected.set(false);
        }
    }

    /**
     * 检查Modbus TCP连接是否已经建立
     *
     * @throws ModbusException 如果连接还没有建立
     */
    private void validateConnection() throws ModbusException {
        if (!isConnected.get()) {
            throw new ModbusException("Not connected");
        }
    }

    /**
     * 从流中读取expectedSize个字节
     *
     * @param in           输入流
     * @param expectedSize 期望的字节数
     * @return 读取的字节
     * @throws IOException 读取时发生IO错误
     */
    public static byte[] readBytes(BufferedInputStream in, int expectedSize) throws IOException {
        // 创建一个字节数组，用于存储读取的字节
        byte[] data = new byte[expectedSize];
        int totalRead = 0; // 记录已经读取的字节数

        // 读取流，直到读取了expectedSize个字节
        while (totalRead < expectedSize) {
            int remaining = expectedSize - totalRead; // 剩余需要读取的字节数
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

    /**
     * 读取Modbus TCP响应
     *
     * @param expectedFunction 读取的Modbus函数码
     * @return 读取的寄存器值
     * @throws IOException     读取时发生IO错误
     * @throws ModbusException 读取的响应格式错误
     */
    private List<Integer> Response(int expectedFunction) throws IOException, ModbusException {
        byte[] header = readBytes(inputStream, ModBuscode.MbapFrameLen);
        List<Integer> data = new ArrayList<>();
        try {
            List<Integer> mbapHeader = ModBuscode.decodeMbapFrame(header);
            byte[] pdu = readBytes(inputStream, mbapHeader.get(2) - ModBuscode.UnitIdLen);
            switch (expectedFunction) {
                case ModBuscode.ReadFunCode:
                    data = ModBuscode.decodeReadReg(pdu);
                    break;
                case ModBuscode.WriteFunCode:
                    data = ModBuscode.decodeWriteReg(pdu);
                    break;
                case ModBuscode.FileFunCode:
                    data = ModBuscode.decodeFileTransport(pdu);
                    break;
            }
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        return data;
    }

    /**
     * 读取Modbus TCP寄存器
     *
     * @param unitId       读取的Modbus单元ID
     * @param startAddress 读取的寄存器起始地址
     * @param quantity     读取寄存器的数量
     * @return 读取的寄存器值
     * @throws IOException     读取时发生IO错误
     * @throws ModbusException 读取的响应格式错误
     */
    public List<Integer> readReg(int unitId, int startAddress, int quantity) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeReadReg(transactionId.incrementAndGet(), unitId, startAddress, quantity);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (lock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                return Response(ModBuscode.ReadFunCode);
            } catch (IOException e) {
                throw new ModbusException("Communication error: " + e.getMessage());
            }
        }
    }

    /**
     * 验证写寄存器的响应
     *
     * @param data         响应数据，包含起始地址和寄存器数量
     * @param startAddress 写操作的起始地址
     * @param quantity     写入的寄存器数量
     * @throws ModbusException 如果起始地址或数量不匹配
     */
    private void validateWriteResponse(List<Integer> data, int startAddress, int quantity)
            throws ModbusException {
        if (data.get(0) != startAddress || data.get(1) != quantity) {
            throw new ModbusException("Write validation failed");
        }
    }

    /**
     * 写Modbus寄存器
     *
     * @param startAddr 写寄存器的起始地址
     * @param values    写寄存器的值
     * @throws ModbusException 如果通信错误或写寄存器的响应格式错误
     */
    public void writeReg(final int startAddr, List<Integer> values) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeWriteReg(transactionId.incrementAndGet(), unitId, startAddr, values);
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

    private void validateFileTransportResponse(List<Integer> data, int fileAddress, int quantity) throws ModbusException {
        if (data.get(0) != fileAddress || data.get(1) != quantity) {
            throw new ModbusException("FileTransport validation failed");
        }
    }

    /**
     * 写Modbus TCP文件传输
     *
     * @param fileAddr 写文件传输的起始地址
     * @param byteData 写入的文件数据
     * @throws ModbusException 如果通信错误或写文件传输的响应格式错误
     */
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


    /**
     * 将一个2字节数据拆分为4字节，按照低字节在前，高字节在后
     * <p>
     * 例如，输入为[0x1234]，那么输出将是[0x34, 0x12]
     *
     * @param value 要拆分的2字节数据
     * @return 拆分后的4字节数据
     */
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
            writeReg(101, values);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            InputStream fis1 = context.getAssets().open("abc.enc");
            while ((bytesRead = fis1.read(buffer)) != -1) {
                // 判断是否为最后一次读取（可能不足缓冲区大小）
                byte[] aligendBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, aligendBytes, 0, bytesRead);
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
            writeReg(101, values);
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
