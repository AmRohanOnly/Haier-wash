package com.openwasher.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.openwasher.app.ble.BleManager;
import com.openwasher.app.data.OrderStorage;
import com.openwasher.app.model.MachineDevice;
import com.openwasher.app.model.MachineOrder;
import com.openwasher.app.model.WashProgram;
import java.util.UUID;

public class PaymentActivity extends Activity {

    private MachineDevice currentDevice;
    private WashProgram selectedProgram;
    private int selectedTemp;
    private int selectedSpin;

    private boolean isFreeMode = true;
    private int selectedAmount = 0;

    private TextView rbFree, rbVoluntary;
    private Button btnTriggerWash;
    private EditText etCoupon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        currentDevice = (MachineDevice) getIntent().getSerializableExtra("device");
        selectedProgram = (WashProgram) getIntent().getSerializableExtra("program");
        selectedTemp = getIntent().getIntExtra("temp", 40);
        selectedSpin = getIntent().getIntExtra("spin", 1000);

        if (currentDevice == null) currentDevice = new MachineDevice("WASHER-01", "Smart Washer #01", "WASHER", "SIMULATED", true);
        if (selectedProgram == null) selectedProgram = WashProgram.getStandardCotton();

        Button btnBack = findViewById(R.id.btnBackPay);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        TextView tvSummaryDetails = findViewById(R.id.tvSummaryDetails);

        tvSummaryTitle.setText(selectedProgram.getName() + " (" + selectedProgram.getDurationMinutes() + " min)");
        tvSummaryDetails.setText("Appliance: " + currentDevice.getId() + " • " + selectedTemp + "°C • " + selectedSpin + " RPM");

        rbFree = findViewById(R.id.rbFree);
        rbVoluntary = findViewById(R.id.rbVoluntary);
        btnTriggerWash = findViewById(R.id.btnTriggerWash);
        etCoupon = findViewById(R.id.etCoupon);

        View optFreeWash = findViewById(R.id.optFreeWash);
        View optVoluntary = findViewById(R.id.optVoluntary);
        Button btnApplyCoupon = findViewById(R.id.btnApplyCoupon);

        optFreeWash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFreeMode = true;
                selectedAmount = 0;
                updateSelectionUI();
            }
        });

        optVoluntary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFreeMode = false;
                selectedAmount = 49;
                updateSelectionUI();
            }
        });

        btnApplyCoupon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etCoupon.getText().toString().trim();
                if (!code.isEmpty()) {
                    isFreeMode = true;
                    selectedAmount = 0;
                    updateSelectionUI();
                    Toast.makeText(PaymentActivity.this, "Coupon '" + code + "' applied: 100% Free Wash", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnTriggerWash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Save order to history
                String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String paymentType = isFreeMode ? "FREE_COMMUNITY" : "VOLUNTARY";
                MachineOrder order = new MachineOrder(
                    orderId,
                    currentDevice.getId(),
                    selectedProgram.getName(),
                    selectedProgram.getDurationMinutes(),
                    selectedAmount,
                    paymentType,
                    System.currentTimeMillis()
                );
                new OrderStorage(PaymentActivity.this).saveOrder(order);

                // 2. Transmit BLE command to physical machine or simulator
                BleManager.getInstance(PaymentActivity.this).sendStart(1, selectedTemp, selectedSpin);

                // 3. Launch live countdown timer
                Intent intent = new Intent(PaymentActivity.this, CycleTimerActivity.class);
                intent.putExtra("device", currentDevice);
                intent.putExtra("program", selectedProgram);
                intent.putExtra("order", order);
                startActivity(intent);
                finish();
            }
        });

        updateSelectionUI();
    }

    private void updateSelectionUI() {
        if (isFreeMode) {
            rbFree.setText("🟢");
            rbVoluntary.setText("⚪");
            btnTriggerWash.setText("🚀 Start Machine Now (₹ 0 Free)");
        } else {
            rbFree.setText("⚪");
            rbVoluntary.setText("🟢");
            btnTriggerWash.setText("🚀 Contribute ₹ 49 & Start Machine");
        }
    }
}
