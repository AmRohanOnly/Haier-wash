package com.openwasher.app.ble;

import java.util.UUID;

public class WasherBleProtocol {
    // Official Haier Smart Appliance GATT Service & Characteristic UUIDs
    public static final UUID SERVICE_WASHER       = UUID.fromString("0000fe60-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_WRITE_COMMAND  = UUID.fromString("0000fe61-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_NOTIFY_STATUS  = UUID.fromString("0000fe62-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_CONFIG         = UUID.fromString("0000fe63-0000-1000-8000-00805f9b34fb");

    // Standard BLE Client Characteristic Configuration Descriptor (CCCD)
    public static final UUID DESCRIPTOR_CCCD     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Auxiliary / UART Services
    public static final UUID SERVICE_ALT_UART    = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");

    // Haier Command Directives
    public static final byte CMD_START_CYCLE  = 0x01;
    public static final byte CMD_PAUSE_CYCLE  = 0x02;
    public static final byte CMD_RESUME_CYCLE = 0x03;
    public static final byte CMD_STOP_CYCLE   = 0x04;
    public static final byte CMD_QUERY_STATUS = 0x05;

    /**
     * Binary frame packet for Haier Smart Washer / Dryer.
     * Header: 0xAA, Directive, Program, Temp, SpinHi, SpinLo, Checksum, Footer: 0x55
     */
    public static byte[] buildStartCommand(int programCode, int tempCelsius, int spinRpm) {
        byte[] packet = new byte[8];
        packet[0] = (byte) 0xAA;
        packet[1] = CMD_START_CYCLE;
        packet[2] = (byte) programCode;
        packet[3] = (byte) tempCelsius;
        packet[4] = (byte) ((spinRpm >> 8) & 0xFF);
        packet[5] = (byte) (spinRpm & 0xFF);
        byte xor = 0;
        for (int i = 0; i < 6; i++) {
            xor ^= packet[i];
        }
        packet[6] = xor;
        packet[7] = (byte) 0x55;
        return packet;
    }

    /**
     * Simple control directive (Pause, Resume, Stop, Query).
     */
    public static byte[] buildSimpleCommand(byte cmd) {
        byte[] packet = new byte[4];
        packet[0] = (byte) 0xAA;
        packet[1] = cmd;
        packet[2] = (byte) (0xAA ^ cmd);
        packet[3] = (byte) 0x55;
        return packet;
    }

    /**
     * Haier ASCII protocol command string (used in commercial laundry serial modules).
     */
    public static byte[] buildAsciiCommand(String commandCode) {
        return (commandCode + "\r\n").getBytes();
    }
}
