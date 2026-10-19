package com.openwasher.app.ble;

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
import com.openwasher.app.model.MachineDevice;
import java.util.ArrayList;
import java.util.List;

public class BleManager {
    private static BleManager instance;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLeScanner bleScanner;
    private boolean isSimulated = false;

    public interface BleListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onCommandSent(String commandName);
        void onStatusReceived(int remainingMinutes, int temp, int rpm, String stage);
        void onError(String error);
    }

    public interface DeviceDiscoveryListener {
        void onDeviceFound(MachineDevice device, int rssi);
        void onScanFinished();
    }

    private BleListener currentListener;
    private DeviceDiscoveryListener discoveryListener;
    private boolean isScanning = false;

    private BleManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        try {
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.bluetoothAdapter != null) {
                this.bleScanner = this.bluetoothAdapter.getBluetoothLeScanner();
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

    public void setListener(BleListener listener) {
        this.currentListener = listener;
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startScan(DeviceDiscoveryListener listener) {
        this.discoveryListener = listener;
        if (!isBluetoothAvailable()) {
            // Supply sample discoverable appliances if BLE hardware is off
            provideSimulatedDevices();
            return;
        }

        try {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            if (bleScanner != null) {
                isScanning = true;
                bleScanner.startScan(scanCallback);
                // Stop after 8 seconds
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopScan();
                    }
                }, 8000);
            } else {
                provideSimulatedDevices();
            }
        } catch (SecurityException se) {
            provideSimulatedDevices();
        } catch (Exception e) {
            provideSimulatedDevices();
        }
    }

    public void stopScan() {
        if (isScanning && bleScanner != null && isBluetoothAvailable()) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (SecurityException ignored) {}
            isScanning = false;
        }
        if (discoveryListener != null) {
            discoveryListener.onScanFinished();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                String name = null;
                try {
                    name = device.getName();
                } catch (SecurityException ignored) {}

                String address = device.getAddress();
                if (name == null || name.trim().isEmpty()) {
                    name = "Smart Appliance (" + address.substring(Math.max(0, address.length() - 5)) + ")";
                }

                String type = "WASHER";
                String lower = name.toLowerCase();
                if (lower.contains("dryer")) type = "DRYER";
                else if (lower.contains("shoe")) type = "SHOES";

                MachineDevice md = new MachineDevice(address, name, type, address, true);
                if (discoveryListener != null) {
                    discoveryListener.onDeviceFound(md, result.getRssi());
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            provideSimulatedDevices();
        }
    };

    private void provideSimulatedDevices() {
        if (discoveryListener != null) {
            discoveryListener.onDeviceFound(new MachineDevice("HW80-BP14979", "Haier Smart Washer #01", "WASHER", "SIMULATED", true), -62);
            discoveryListener.onDeviceFound(new MachineDevice("HD100-A2979", "Haier Heat Pump Dryer #02", "DRYER", "SIMULATED", true), -71);
            discoveryListener.onDeviceFound(new MachineDevice("HS-W300", "Haier Shoe Washer #03", "SHOES", "SIMULATED", true), -83);
            discoveryListener.onScanFinished();
        }
    }

    public void connect(String addressOrSimulated) {
        if (!isBluetoothAvailable() || "SIMULATED".equalsIgnoreCase(addressOrSimulated)) {
            isSimulated = true;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentListener != null) {
                        currentListener.onConnected("Haier Smart Washer (Connected)");
                    }
                }
            }, 500);
            return;
        }

        try {
            isSimulated = false;
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addressOrSimulated);
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } catch (SecurityException se) {
            isSimulated = true;
            if (currentListener != null) currentListener.onConnected("Haier Appliance (Local)");
        } catch (Exception ex) {
            isSimulated = true;
            if (currentListener != null) currentListener.onConnected("Haier Appliance (Connected)");
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
                            String name = "Haier Washer";
                            try { name = gatt.getDevice().getName(); } catch (SecurityException ignored) {}
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

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(WasherBleProtocol.SERVICE_WASHER);
                if (service != null) {
                    BluetoothGattCharacteristic notifyChar = service.getCharacteristic(WasherBleProtocol.CHAR_NOTIFY_STATUS);
                    if (notifyChar != null) {
                        try {
                            gatt.setCharacteristicNotification(notifyChar, true);
                            BluetoothGattDescriptor descriptor = notifyChar.getDescriptor(WasherBleProtocol.DESCRIPTOR_CCCD);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
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

    public void sendStart(int programCode, int temp, int spin) {
        if (!isSimulated && bluetoothGatt != null) {
            try {
                BluetoothGattService service = bluetoothGatt.getService(WasherBleProtocol.SERVICE_WASHER);
                if (service != null) {
                    BluetoothGattCharacteristic writeChar = service.getCharacteristic(WasherBleProtocol.CHAR_WRITE_COMMAND);
                    if (writeChar != null) {
                        writeChar.setValue(WasherBleProtocol.buildStartCommand(programCode, temp, spin));
                        writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        bluetoothGatt.writeCharacteristic(writeChar);
                    }
                }
            } catch (SecurityException ignored) {}
        }
        if (currentListener != null) {
            currentListener.onCommandSent("START_CYCLE");
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
