package com.openwasher.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.openwasher.app.model.MachineDevice;

public class ScannerActivity extends Activity {

    private EditText etMachineCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Button btnBack = findViewById(R.id.btnBack);
        etMachineCode = findViewById(R.id.etMachineCode);
        Button btnConnectManual = findViewById(R.id.btnConnectManual);
        Button btnPresetWasher1 = findViewById(R.id.btnPresetWasher1);
        Button btnPresetDryer = findViewById(R.id.btnPresetDryer);

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
                    Toast.makeText(ScannerActivity.this, "Please enter a machine ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                proceedWithMachine(code, "Connected Appliance " + code);
            }
        });

        btnPresetWasher1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceedWithMachine("WASHER-01", "Haier Smart Washer #01");
            }
        });

        btnPresetDryer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proceedWithMachine("DRYER-02", "Haier Commercial Dryer #02");
            }
        });
    }

    private void proceedWithMachine(String id, String name) {
        MachineDevice device = new MachineDevice(id, name, "WASHER", "SIMULATED", true);
        Intent intent = new Intent(ScannerActivity.this, WashControlActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        finish();
    }
}
