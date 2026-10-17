package com.openwasher.app.ble;

import java.util.UUID;

public class WasherBleProtocol {
    // Appliance GATT Service & Characteristics UUIDs
    public static final UUID SERVICE_WASHER = UUID.fromString("0000fe60-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_WRITE_COMMAND = UUID.fromString("0000fe61-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_NOTIFY_STATUS = UUID.fromString("0000fe62-0000-1000-8000-00805f9b34fb");

    // Command Codes
    public static final byte CMD_START_CYCLE  = 0x01;
    public static final byte CMD_PAUSE_CYCLE  = 0x02;
    public static final byte CMD_RESUME_CYCLE = 0x03;
    public static final byte CMD_STOP_CYCLE   = 0x04;
    public static final byte CMD_QUERY_STATUS = 0x05;

    public static byte[] buildStartCommand(int programCode, int tempCelsius, int spinRpm) {
        byte[] packet = new byte[8];
        packet[0] = (byte) 0xAA; // Frame start
        packet[1] = CMD_START_CYCLE;
        packet[2] = (byte) programCode;
        packet[3] = (byte) tempCelsius;
        packet[4] = (byte) ((spinRpm >> 8) & 0xFF);
        packet[5] = (byte) (spinRpm & 0xFF);
        // Checksum XOR
        byte xor = 0;
        for (int i = 0; i < 6; i++) {
            xor ^= packet[i];
        }
        packet[6] = xor;
        packet[7] = (byte) 0x55; // Frame end
        return packet;
    }

    public static byte[] buildSimpleCommand(byte cmd) {
        byte[] packet = new byte[4];
        packet[0] = (byte) 0xAA;
        packet[1] = cmd;
        packet[2] = (byte) (0xAA ^ cmd);
        packet[3] = (byte) 0x55;
        return packet;
    }
}
