package com.openwasher.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.openwasher.app.ble.BleManager;
import com.openwasher.app.model.MachineDevice;
import java.util.HashSet;
import java.util.Set;

public class ScannerActivity extends Activity {

    private EditText etMachineCode;
    private LinearLayout llDiscoveredDevices;
    private ProgressBar pbBleScanning;
    private TextView tvBleStatus;
    private Set<String> discoveredMacs = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Button btnBack = findViewById(R.id.btnBack);
        etMachineCode = findViewById(R.id.etMachineCode);
        Button btnConnectManual = findViewById(R.id.btnConnectManual);
        llDiscoveredDevices = findViewById(R.id.llDiscoveredDevices);
        pbBleScanning = findViewById(R.id.pbBleScanning);
        tvBleStatus = findViewById(R.id.tvBleStatus);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnConnectManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etMachineCode.getText().toString().trim();
                if (TextUtils.isEmpty(code)) {
                    Toast.makeText(ScannerActivity.this, "Please enter a machine ID or MAC", Toast.LENGTH_SHORT).show();
                    return;
                }
                proceedWithMachine(code, "Haier Appliance " + code, code);
            }
        });

        startBleApplianceScan();
    }

    private void startBleApplianceScan() {
        discoveredMacs.clear();
        llDiscoveredDevices.removeAllViews();
        pbBleScanning.setVisibility(View.VISIBLE);
        tvBleStatus.setText("Scanning for Haier BLE signals within 10m…");

        BleManager.getInstance(this).startScan(new BleManager.DeviceDiscoveryListener() {
            @Override
            public void onDeviceFound(final MachineDevice device, final int rssi) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addDiscoveredDeviceCard(device, rssi);
                    }
                });
            }

            @Override
            public void onScanFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbBleScanning.setVisibility(View.GONE);
                        if (discoveredMacs.isEmpty()) {
                            tvBleStatus.setText("Scan complete. No nearby signals detected.");
                        } else {
                            tvBleStatus.setText("Found " + discoveredMacs.size() + " nearby smart appliance(s)");
                        }
                    }
                });
            }
        });
    }

    private void addDiscoveredDeviceCard(final MachineDevice device, int rssi) {
        if (discoveredMacs.contains(device.getBleAddress())) {
            return;
        }
        discoveredMacs.add(device.getBleAddress());

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setBackgroundResource(R.drawable.bg_card_rounded);
        item.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        item.setLayoutParams(lp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textLp);

        TextView tvName = new TextView(this);
        tvName.setText(device.getName());
        tvName.setTextColor(getResources().getColor(R.color.text_primary));
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvSub = new TextView(this);
        tvSub.setText("MAC: " + device.getBleAddress() + " • Signal: " + rssi + " dBm");
        tvSub.setTextColor(getResources().getColor(R.color.accent));
        tvSub.setTextSize(12);

        textCol.addView(tvName);
        textCol.addView(tvSub);

        Button btnConnect = new Button(this);
        btnConnect.setText("Connect");
        btnConnect.setTextColor(getResources().getColor(R.color.white));
        btnConnect.setBackgroundResource(R.drawable.bg_button_primary);
        btnConnect.setPadding(32, 0, 32, 0);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceedWithMachine(device.getId(), device.getName(), device.getBleAddress());
            }
        });

        item.addView(textCol);
        item.addView(btnConnect);
        llDiscoveredDevices.addView(item);
    }

    private void proceedWithMachine(String id, String name, String bleAddress) {
        BleManager.getInstance(this).stopScan();
        MachineDevice device = new MachineDevice(id, name, "WASHER", bleAddress, true);
        Intent intent = new Intent(ScannerActivity.this, WashControlActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance(this).stopScan();
    }
}
