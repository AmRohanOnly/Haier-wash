package com.openwasher.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.openwasher.app.ble.BleManager;
import com.openwasher.app.model.MachineDevice;
import com.openwasher.app.model.WashProgram;

public class WashControlActivity extends Activity {

    private MachineDevice currentDevice;
    private WashProgram selectedProgram;
    private int selectedTemp = 40;
    private int selectedSpin = 1000;

    private LinearLayout progQuick, progStandard, progHeavy, progShoes;
    private TextView tvDeviceTitle, tvDeviceSub, tvSelectedTemp, tvSelectedSpin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wash_control);

        currentDevice = (MachineDevice) getIntent().getSerializableExtra("device");
        if (currentDevice == null) {
            currentDevice = new MachineDevice("WASHER-01", "Haier Smart Washer #01", "WASHER", "SIMULATED", true);
        }

        selectedProgram = WashProgram.getStandardCotton();

        tvDeviceTitle = findViewById(R.id.tvDeviceTitle);
        tvDeviceSub = findViewById(R.id.tvDeviceSub);
        tvSelectedTemp = findViewById(R.id.tvSelectedTemp);
        tvSelectedSpin = findViewById(R.id.tvSelectedSpin);

        tvDeviceTitle.setText(currentDevice.getName());
        tvDeviceSub.setText("Unit " + currentDevice.getId() + " • Connected via BLE");

        Button btnBack = findViewById(R.id.btnBackControl);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        progQuick = findViewById(R.id.progQuick);
        progStandard = findViewById(R.id.progStandard);
        progHeavy = findViewById(R.id.progHeavy);
        progShoes = findViewById(R.id.progShoes);

        setupProgramListeners();
        highlightSelectedProgram();

        // Connect BLE in background
        BleManager.getInstance(this).connect(currentDevice.getBleAddress());

        Button btnProceed = findViewById(R.id.btnProceedOptions);
        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WashControlActivity.this, PaymentActivity.class);
                intent.putExtra("device", currentDevice);
                intent.putExtra("program", selectedProgram);
                intent.putExtra("temp", selectedTemp);
                intent.putExtra("spin", selectedSpin);
                startActivity(intent);
            }
        });
    }

    private void setupProgramListeners() {
        progQuick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedProgram = WashProgram.getQuickWash();
                selectedTemp = 30;
                selectedSpin = 800;
                highlightSelectedProgram();
            }
        });

        progStandard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedProgram = WashProgram.getStandardCotton();
                selectedTemp = 40;
                selectedSpin = 1000;
                highlightSelectedProgram();
            }
        });

        progHeavy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedProgram = WashProgram.getHeavyDuty();
                selectedTemp = 60;
                selectedSpin = 1200;
                highlightSelectedProgram();
            }
        });

        progShoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedProgram = WashProgram.getShoeClean();
                selectedTemp = 30;
                selectedSpin = 700;
                highlightSelectedProgram();
            }
        });
    }

    private void highlightSelectedProgram() {
        progQuick.setAlpha(selectedProgram.getId().equals("quick") ? 1.0f : 0.6f);
        progStandard.setAlpha(selectedProgram.getId().equals("standard") ? 1.0f : 0.6f);
        progHeavy.setAlpha(selectedProgram.getId().equals("heavy") ? 1.0f : 0.6f);
        progShoes.setAlpha(selectedProgram.getId().equals("shoes") ? 1.0f : 0.6f);

        tvSelectedTemp.setText(selectedTemp + "°C");
        tvSelectedSpin.setText(selectedSpin + " RPM");
    }
}
