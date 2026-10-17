package com.openwasher.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.openwasher.app.data.OrderStorage;
import com.openwasher.app.model.MachineDevice;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQ_CODE = 101;
    private TextView tvTotalWashes;
    private TextView tvFreeSaved;
    private OrderStorage orderStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        orderStorage = new OrderStorage(this);

        tvTotalWashes = findViewById(R.id.tvTotalWashes);
        tvFreeSaved = findViewById(R.id.tvFreeSaved);

        Button btnScanQr = findViewById(R.id.btnScanQr);
        Button btnQuickConnect = findViewById(R.id.btnQuickConnect);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);

        View cardWasher = findViewById(R.id.cardWasher);
        View cardDryer = findViewById(R.id.cardDryer);
        View cardShoeWasher = findViewById(R.id.cardShoeWasher);

        btnScanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivity(intent);
            }
        });

        btnQuickConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlScreen(new MachineDevice("WASHER-01", "Haier Smart Washer #01", "WASHER", "SIMULATED", true));
            }
        });

        cardWasher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlScreen(new MachineDevice("WASHER-01", "Haier Smart Washer #01", "WASHER", "SIMULATED", true));
            }
        });

        cardDryer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlScreen(new MachineDevice("DRYER-02", "Haier Smart Dryer #02", "DRYER", "SIMULATED", true));
            }
        });

        cardShoeWasher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlScreen(new MachineDevice("SHOE-03", "Haier Shoe Washer #03", "SHOES", "SIMULATED", true));
            }
        });

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OrderHistoryActivity.class));
            }
        });

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> needed = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.BLUETOOTH_SCAN);
                }
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.BLUETOOTH_CONNECT);
                }
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.CAMERA);
            }
            if (!needed.isEmpty()) {
                requestPermissions(needed.toArray(new String[0]), PERMISSION_REQ_CODE);
            }
        }
    }

    private void startControlScreen(MachineDevice device) {
        Intent intent = new Intent(MainActivity.this, WashControlActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvTotalWashes.setText(String.valueOf(orderStorage.getTotalWashes()));
        tvFreeSaved.setText("₹ " + orderStorage.getTotalSavings());
    }
}
