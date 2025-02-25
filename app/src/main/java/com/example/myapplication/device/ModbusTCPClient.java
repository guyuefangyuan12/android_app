package com.example.myapplication.device;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.net.InetAddress;

public class ModbusTCPClient {
    private TCPMasterConnection connection;
    private String ip;
    private int port;

    public ModbusTCPClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean connect() {
        try {
            // 创建InetAddress对象
            InetAddress address = InetAddress.getByName(ip);

            // 创建TCPMasterConnection对象
            connection = new TCPMasterConnection(address);
            connection.setPort(port);

            // 连接到ModBus服务器
            connection.connect();

            // 设置ModBus协议模式为TCP
            //connection.setModbusTransport(new com.ghgande.j2mod.modbus.io.ModbusTCPTransport(connection));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int[] readInputRegisters(int unitId, int reference, int count) {
        try {
            // 创建ModbusTCPTransaction对象
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);

            // 创建ReadInputRegistersRequest对象
            ReadInputRegistersRequest request = new ReadInputRegistersRequest(reference, count);
            request.setUnitID(unitId);

            // 设置请求
            transaction.setRequest(request);

            // 执行请求
            transaction.execute();

            // 获取响应
            ReadInputRegistersResponse response = (ReadInputRegistersResponse) transaction.getResponse();

            // 获取寄存器值
            int[] registers = new int[count];
            for (int i = 0; i < count; i++) {
                registers[i] = response.getRegister(i).getValue();
            }
            return registers;
        } catch (Exception e) {
            System.err.println("IO exception occurred while reading input registers: " + e.getMessage());
            // 或者记录日志
            // logger.error("IO exception occurred while reading input registers", e);
        }
        // 返回一个空数组表示失败
        return new int[0];
    }

    // 其他ModBus操作可以在这里添加
}
