package com.openwasher.app;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.openwasher.app.ble.BleManager;
import com.openwasher.app.model.MachineDevice;
import java.util.ArrayList;
import java.util.List;

public class ScannerActivity extends Activity {

    private EditText etMachineCode;
    private TextView tvScanStatus;
    private ProgressBar pbScanning;
    private Button btnStartBleScan;
    private ListView lvDiscovered;

    private DeviceListAdapter adapter;
    private List<DiscoveredItem> discoveredList = new ArrayList<>();

    private static class DiscoveredItem {
        String name;
        String address;
        int rssi;
        boolean isHaier;

        DiscoveredItem(String name, String address, int rssi, boolean isHaier) {
            this.name = name;
            this.address = address;
            this.rssi = rssi;
            this.isHaier = isHaier;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Button btnBack = findViewById(R.id.btnBack);
        etMachineCode = findViewById(R.id.etMachineCode);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        pbScanning = findViewById(R.id.pbScanning);
        btnStartBleScan = findViewById(R.id.btnStartBleScan);
        lvDiscovered = findViewById(R.id.lvDiscovered);
        Button btnConnectManual = findViewById(R.id.btnConnectManual);

        adapter = new DeviceListAdapter();
        lvDiscovered.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Seed with preset Haier machines if none found yet
        discoveredList.add(new DiscoveredItem("Haier Smart Washer #01 (BLE Demo)", "SIMULATED", -52, true));
        discoveredList.add(new DiscoveredItem("Haier Commercial Dryer #02 (BLE Demo)", "SIMULATED", -68, true));
        adapter.notifyDataSetChanged();

        btnStartBleScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });

        lvDiscovered.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DiscoveredItem item = discoveredList.get(position);
                proceedWithMachine(item.address, item.name);
            }
        });

        btnConnectManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etMachineCode.getText().toString().trim();
                if (TextUtils.isEmpty(code)) {
                    Toast.makeText(ScannerActivity.this, "Please enter a machine ID or MAC address", Toast.LENGTH_SHORT).show();
                    return;
                }
                proceedWithMachine(code, "Appliance " + code);
            }
        });

        // Automatically start live scan on open
        startScanning();
    }

    private void startScanning() {
        pbScanning.setVisibility(View.VISIBLE);
        tvScanStatus.setText("Scanning for Bluetooth Low Energy appliances (10s)...");
        btnStartBleScan.setEnabled(false);

        BleManager.getInstance(this).startLeScan(new BleManager.ScanDeviceListener() {
            @Override
            public void onDeviceFound(BluetoothDevice device, int rssi, String name) {
                String devName = name != null ? name : "Device";
                boolean isHaier = devName.toLowerCase().contains("haier") ||
                                  devName.toLowerCase().contains("washer") ||
                                  devName.toLowerCase().contains("dryer") ||
                                  devName.toLowerCase().contains("sw_") ||
                                  devName.toLowerCase().contains("uplug");

                // Check if already in list
                boolean exists = false;
                for (DiscoveredItem it : discoveredList) {
                    if (it.address.equalsIgnoreCase(device.getAddress())) {
                        it.rssi = rssi;
                        it.name = devName;
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    discoveredList.add(0, new DiscoveredItem(devName, device.getAddress(), rssi, isHaier));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished() {
                pbScanning.setVisibility(View.GONE);
                btnStartBleScan.setEnabled(true);
                tvScanStatus.setText("Scan completed. Tap an appliance to connect.");
            }
        }, 10000);
    }

    private void proceedWithMachine(String address, String name) {
        String type = name.toLowerCase().contains("dryer") ? "DRYER" : "WASHER";
        MachineDevice device = new MachineDevice(address, name, type, address, true);
        Intent intent = new Intent(ScannerActivity.this, WashControlActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        finish();
    }

    private class DeviceListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return discoveredList.size(); }
        @Override
        public Object getItem(int pos) { return discoveredList.get(pos); }
        @Override
        public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.item_history, parent, false);
            }
            DiscoveredItem item = discoveredList.get(pos);

            TextView tvName = convertView.findViewById(R.id.tvHistoryProgram);
            TextView tvBadge = convertView.findViewById(R.id.tvHistoryCost);
            TextView tvMac = convertView.findViewById(R.id.tvHistoryMachine);
            TextView tvRssi = convertView.findViewById(R.id.tvHistoryDate);

            tvName.setText((item.isHaier ? "🧺 " : "📡 ") + item.name);
            tvBadge.setText(item.isHaier ? "HAIER BLE" : "BLUETOOTH");
            tvBadge.setTextColor(getResources().getColor(item.isHaier ? R.color.accent_glow : R.color.text_muted));
            tvMac.setText("MAC / ID: " + item.address);
            tvRssi.setText("Signal: " + item.rssi + " dBm");

            return convertView;
        }
    }
}
