package com.openwasher.app.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleManager {
    private static BleManager instance;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isSimulated = false;

    // Standard Client Characteristic Configuration Descriptor
    public static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public interface BleListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onCommandSent(String commandName);
        void onStatusReceived(int remainingMinutes, int temp, int rpm, String stage);
        void onError(String error);
    }

    public interface ScanDeviceListener {
        void onDeviceFound(BluetoothDevice device, int rssi, String name);
        void onScanFinished();
    }

    private BleListener currentListener;

    private BleManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        try {
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.bluetoothAdapter != null) {
                this.leScanner = this.bluetoothAdapter.getBluetoothLeScanner();
            }
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

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void setListener(BleListener listener) {
        this.currentListener = listener;
    }

    @SuppressLint("MissingPermission")
    public void startLeScan(final ScanDeviceListener listener, int timeoutMs) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (listener != null) listener.onScanFinished();
            return;
        }

        try {
            if (leScanner == null) {
                leScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            if (leScanner == null) {
                if (listener != null) listener.onScanFinished();
                return;
            }

            final List<String> seenMacs = new ArrayList<>();
            final ScanCallback callback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice dev = result.getDevice();
                    if (dev != null && !seenMacs.contains(dev.getAddress())) {
                        seenMacs.add(dev.getAddress());
                        String name = dev.getName();
                        if (listener != null) {
                            listener.onDeviceFound(dev, result.getRssi(), name != null ? name : "Appliance (" + dev.getAddress() + ")");
                        }
                    }
                }
            };

            leScanner.startScan(callback);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        leScanner.stopScan(callback);
                    } catch (Exception ignored) {}
                    if (listener != null) {
                        listener.onScanFinished();
                    }
                }
            }, timeoutMs);

        } catch (SecurityException se) {
            if (listener != null) listener.onScanFinished();
        } catch (Exception ex) {
            if (listener != null) listener.onScanFinished();
        }
    }

    @SuppressLint("MissingPermission")
    public void connect(String addressOrSimulated) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || "SIMULATED".equalsIgnoreCase(addressOrSimulated)) {
            isSimulated = true;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentListener != null) {
                        currentListener.onConnected("Haier SmartWasher (Simulator)");
                    }
                }
            }, 500);
            return;
        }

        try {
            isSimulated = false;
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addressOrSimulated);
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
        } catch (SecurityException se) {
            isSimulated = true;
            if (currentListener != null) {
                currentListener.onConnected("Haier Washer (Simulated Mode)");
            }
        } catch (Exception ex) {
            isSimulated = true;
            if (currentListener != null) {
                currentListener.onConnected("Haier Washer (Fallback)");
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
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
                            String name = gatt.getDevice().getName();
                            currentListener.onConnected(name != null ? name : "Haier Appliance");
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (currentListener != null) {
                            currentListener.onDisconnected();
                        }
                    }
                }
            });
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(WasherBleProtocol.SERVICE_WASHER);
                if (service != null) {
                    BluetoothGattCharacteristic notifyChar = service.getCharacteristic(WasherBleProtocol.CHAR_NOTIFY_STATUS);
                    if (notifyChar != null) {
                        try {
                            gatt.setCharacteristicNotification(notifyChar, true);
                            BluetoothGattDescriptor desc = notifyChar.getDescriptor(CCCD_UUID);
                            if (desc != null) {
                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(desc);
                            }
                        } catch (SecurityException ignored) {}
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] val = characteristic.getValue();
            if (val != null && val.length >= 4) {
                final int remaining = val[2] & 0xFF;
                final int temp = val[3] & 0xFF;
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

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
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
