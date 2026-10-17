package com.openwasher.app.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class BleManager {
    private static BleManager instance;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private boolean isSimulated = false;

    public interface BleListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onCommandSent(String commandName);
        void onStatusReceived(int remainingMinutes, int temp, int rpm, String stage);
        void onError(String error);
    }

    private BleListener currentListener;

    private BleManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        try {
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            this.bluetoothAdapter = null;
        }
    }

    public static synchronized BleManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new BleManager(ctx);
        }
        return instance;
    }

    public void setListener(BleListener listener) {
        this.currentListener = listener;
    }

    public void connect(String addressOrSimulated) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || "SIMULATED".equalsIgnoreCase(addressOrSimulated)) {
            // High fidelity Hardware Simulator mode
            isSimulated = true;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentListener != null) {
                        currentListener.onConnected("SmartWasher (Simulator)");
                    }
                }
            }, 600);
            return;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addressOrSimulated);
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
        } catch (SecurityException se) {
            isSimulated = true;
            if (currentListener != null) {
                currentListener.onConnected("SmartWasher (Local Mode)");
            }
        } catch (Exception ex) {
            isSimulated = true;
            if (currentListener != null) {
                currentListener.onConnected("SmartWasher (Fallback)");
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        try {
                            gatt.discoverServices();
                        } catch (SecurityException ignored) {}
                        if (currentListener != null) {
                            currentListener.onConnected("Appliance " + gatt.getDevice().getName());
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (currentListener != null) {
                            currentListener.onDisconnected();
                        }
                    }
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(WasherBleProtocol.SERVICE_WASHER);
                if (service != null) {
                    BluetoothGattCharacteristic notifyChar = service.getCharacteristic(WasherBleProtocol.CHAR_NOTIFY_STATUS);
                    if (notifyChar != null) {
                        try {
                            gatt.setCharacteristicNotification(notifyChar, true);
                        } catch (SecurityException ignored) {}
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] val = characteristic.getValue();
            if (val != null && val.length >= 4) {
                int remaining = val[2] & 0xFF;
                int temp = val[3] & 0xFF;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (currentListener != null) {
                            currentListener.onStatusReceived(remaining, temp, 1000, "Washing");
                        }
                    }
                });
            }
        }
    };

    public void sendStart(int programCode, int temp, int rpm) {
        if (isSimulated || bluetoothGatt == null) {
            if (currentListener != null) {
                currentListener.onCommandSent("START_CYCLE");
            }
            return;
        }

        try {
            BluetoothGattService service = bluetoothGatt.getService(WasherBleProtocol.SERVICE_WASHER);
            if (service != null) {
                BluetoothGattCharacteristic writeChar = service.getCharacteristic(WasherBleProtocol.CHAR_WRITE_COMMAND);
                if (writeChar != null) {
                    writeChar.setValue(WasherBleProtocol.buildStartCommand(programCode, temp, rpm));
                    bluetoothGatt.writeCharacteristic(writeChar);
                }
            }
            if (currentListener != null) {
                currentListener.onCommandSent("START_CYCLE");
            }
        } catch (SecurityException se) {
            if (currentListener != null) {
                currentListener.onCommandSent("START_CYCLE (Simulated)");
            }
        }
    }

    public void sendPause() {
        sendSimple(WasherBleProtocol.CMD_PAUSE_CYCLE, "PAUSE");
    }

    public void sendResume() {
        sendSimple(WasherBleProtocol.CMD_RESUME_CYCLE, "RESUME");
    }

    public void sendStop() {
        sendSimple(WasherBleProtocol.CMD_STOP_CYCLE, "STOP");
    }

    private void sendSimple(byte cmd, String name) {
        if (!isSimulated && bluetoothGatt != null) {
            try {
                BluetoothGattService service = bluetoothGatt.getService(WasherBleProtocol.SERVICE_WASHER);
                if (service != null) {
                    BluetoothGattCharacteristic writeChar = service.getCharacteristic(WasherBleProtocol.CHAR_WRITE_COMMAND);
                    if (writeChar != null) {
                        writeChar.setValue(WasherBleProtocol.buildSimpleCommand(cmd));
                        bluetoothGatt.writeCharacteristic(writeChar);
                    }
                }
            } catch (SecurityException ignored) {}
        }
        if (currentListener != null) {
            currentListener.onCommandSent(name);
        }
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (SecurityException ignored) {}
            bluetoothGatt = null;
        }
    }
}
