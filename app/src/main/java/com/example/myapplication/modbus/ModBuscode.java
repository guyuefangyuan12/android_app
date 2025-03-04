package com.example.myapplication.modbus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ModBuscode {
    public static final int ReadFunCode = 0x03;
    public static final int WriteFunCode = 0x10;
    public static final int FileFunCode = 0x26;
    public static final int ErrorFunCode = 0x80;
    public static final int MbapFrameLen = 7;
    public static final int MbapCountLen = 2;
    public static final int UnitIdLen = 1;
    public static final int FunCodeLen = 1;
    public static final int RegAddrLen = 2;
    public static final int RegDataLen = 2;
    public static final int RegCountLen = 2;
    public static final int ReadResponseCountLen = 1;
    public static final int WriteRequestCountLen = 1;
    public static final int FileAddrLen = 4;
    public static final int FileDataCountLen = 2;
    public static final int FileDataLen = 1;

    public static class ModbusFrameException extends Exception {
        public ModbusFrameException(String message) {
            super(message);
        }
    }

    private static byte[] createMbapHeader(int transactionId, int unitId, int pduLength) {
        ByteBuffer buffer = ByteBuffer.allocate(MbapFrameLen)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort((short) transactionId)
                .putShort((short) 0x0000)    // 协议标识符
                .putShort((short) (pduLength + 1)) // 长度（包含单元ID）
                .put((byte) unitId);
        return buffer.array();
    }

    public static int[] parseMbapFrame(byte[] frame) throws ModbusFrameException {
        if (frame.length < MbapFrameLen) {
            throw new ModbusFrameException("Frame too short (" + frame.length + " bytes)");
        }

        ByteBuffer buffer = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        int[] mbapHeader = new int[3];
        mbapHeader[0] = buffer.getShort() & 0xFFFF;
        mbapHeader[1] = buffer.get(6) & 0xFF;
        mbapHeader[2] = buffer.getShort(4) & 0xFFFF;
        return mbapHeader;
    }

    public static class MbapHeader {
        public final int transactionId;
        public final int unitId;
        public final int length;

        public MbapHeader(int transactionId, int unitId, int length) {
            this.transactionId = transactionId;
            this.unitId = unitId;
            this.length = length;
        }
    }

    private static void checkException(byte functionCode) throws ModbusFrameException {
        if ((functionCode & ErrorFunCode) != 0) {
            throw new ModbusFrameException("Modbus exception: 0x" +
                    Integer.toHexString(functionCode & 0xFF));
        }
    }

    public static byte[] encodeReadHoldingRegisters(
            int transactionId,
            int unitId,
            int startAddress,
            int quantity
    ) throws ModbusFrameException {
        if (quantity < 1 || quantity > 125) {
            throw new ModbusFrameException("Quantity out of range (1-125)");
        }

        ByteBuffer pdu = ByteBuffer.allocate(FunCodeLen + RegAddrLen + RegCountLen)
                .put((byte) ReadFunCode)
                .putShort((short) startAddress)
                .putShort((short) quantity);

        return ByteBuffer.allocate(MbapFrameLen + FunCodeLen + RegAddrLen + RegCountLen)
                .put(createMbapHeader(transactionId, unitId, 5))
                .put(pdu.array())
                .array();
    }

    public static List<Integer> decodeReadHoldingRegisters(byte[] pdu, int length) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        checkException(pdu[0]);
        if ((pdu[0] & 0xFF) != ReadFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int byteCount = pdu[1] & 0xFF;
        if (pdu.length != FunCodeLen + ReadResponseCountLen + byteCount) {
            throw new ModbusFrameException("Invalid byte count");
        }

        List<Integer> registers = new ArrayList<>();
        for (int i = 0; i < byteCount; i += RegDataLen) {
            int pos = 2 + i;
            int data = 0;
            for (int j = 0; j < RegDataLen; j++) {
                data |= (pdu[pos + j] << (8 * (RegDataLen - 1 - j)));
            }
            registers.add(data);
        }
        return registers;
    }

    public static byte[] encodeWriteMultipleRegisters(
            int transactionId,
            int unitId,
            int startAddress,
            List<Integer> values
    ) throws ModbusFrameException {
        if (values.size() < 1 || values.size() > 1200) {
            throw new ModbusFrameException("Values count out of range (1-123)");
        }

        ByteBuffer pdu = ByteBuffer.allocate(FunCodeLen + RegAddrLen + RegCountLen + WriteRequestCountLen + values.size() * RegDataLen)
                .put((byte) WriteFunCode)
                .putShort((short) startAddress)
                .putShort((short) values.size())
                .put((byte) (values.size() * RegDataLen));

        for (int value : values) {
            pdu.putShort((short) value);
        }

        return ByteBuffer.allocate(MbapFrameLen + pdu.position())
                .put(createMbapHeader(transactionId, unitId, pdu.position()))
                .put(pdu.array(), 0, pdu.position())
                .array();
    }

    public static List<Integer> decodeWriteMultipleRegisters(byte[] pdu, int length) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        checkException(pdu[0]);
        if ((pdu[0] & 0xFF) != WriteFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int startAddress = 0;
        int quantity = 0;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < RegAddrLen; i++) {
            startAddress |= (pdu[1 + i] << (8 * (RegAddrLen - 1 - i)));
        }
        for (int i = RegCountLen - 1; i >= 0; i--) {
            quantity |= (pdu[1 + RegAddrLen + i] << (8 * (RegDataLen - 1 - i)));
        }
        result.add(startAddress);
        result.add(quantity);
        return result;
    }

    public static byte[] encodeFileTransport(
            int transactionId,
            int unitId,
            int FileAddress,
            byte[] fileData
    ) throws ModbusFrameException {
        if (fileData.length < 1 || fileData.length > 1400) {
            throw new ModbusFrameException("fileData count out of range (1-1200)");
        }
        ByteBuffer pdu = ByteBuffer.allocate(FunCodeLen + FileAddrLen + FileDataCountLen + fileData.length * FileDataLen)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) FileFunCode)
                .putInt(FileAddress)
                .putShort((short) fileData.length);
        for (byte byteData : fileData) {
            pdu.put(byteData);
        }
        return ByteBuffer.allocate(MbapFrameLen + pdu.position())
                .put(createMbapHeader(transactionId, unitId, pdu.position()))
                .put(pdu.array(), 0, pdu.position())
                .array();
    }

    public static List<Integer> decodeFileTransport(byte[] pdu, int length) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        checkException(pdu[0]);
        if ((pdu[0] & 0xFF) != FileFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int fileAddress = 0;
        int quantity = 0;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < FileAddrLen; i++) {
            fileAddress += (pdu[1 + i] << (8 * i));
        }
        for (int i = 0; i < FileDataCountLen; i++) {
            quantity += (pdu[1 + FileAddrLen + i] << (8 * i));
        }
        result.add(fileAddress);
        result.add(quantity);
        return result;
    }
}


