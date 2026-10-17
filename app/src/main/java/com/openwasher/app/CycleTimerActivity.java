package com.openwasher.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.openwasher.app.ble.BleManager;
import com.openwasher.app.model.MachineDevice;
import com.openwasher.app.model.WashProgram;

public class CycleTimerActivity extends Activity {

    private MachineDevice currentDevice;
    private WashProgram currentProgram;

    private TextView tvCountdown, tvDrumPhase, tvStagePercent, tvCycleDetailSub, tvTimerMachineTitle;
    private ProgressBar pbCycleProgress;
    private View viewDrumOuter;
    private Button btnPauseResume, btnEmergencyStop;

    private CountDownTimer countDownTimer;
    private long totalDurationMillis;
    private long millisRemaining;
    private boolean isPaused = false;
    private RotateAnimation drumAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_timer);

        currentDevice = (MachineDevice) getIntent().getSerializableExtra("device");
        currentProgram = (WashProgram) getIntent().getSerializableExtra("program");

        if (currentDevice == null) currentDevice = new MachineDevice("WASHER-01", "Smart Washer #01", "WASHER", "SIMULATED", true);
        if (currentProgram == null) currentProgram = WashProgram.getStandardCotton();

        tvTimerMachineTitle = findViewById(R.id.tvTimerMachineTitle);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvDrumPhase = findViewById(R.id.tvDrumPhase);
        tvStagePercent = findViewById(R.id.tvStagePercent);
        tvCycleDetailSub = findViewById(R.id.tvCycleDetailSub);
        pbCycleProgress = findViewById(R.id.pbCycleProgress);
        viewDrumOuter = findViewById(R.id.viewDrumOuter);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEmergencyStop = findViewById(R.id.btnEmergencyStop);

        tvTimerMachineTitle.setText(currentDevice.getId() + " • " + currentProgram.getName());

        Button btnClose = findViewById(R.id.btnBackTimer);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set up drum rotation animation
        drumAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        drumAnimation.setDuration(2400);
        drumAnimation.setRepeatCount(Animation.INFINITE);
        viewDrumOuter.startAnimation(drumAnimation);

        // Duration in millis
        totalDurationMillis = currentProgram.getDurationMinutes() * 60 * 1000L;
        millisRemaining = totalDurationMillis;

        startTimer(millisRemaining);

        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPaused) {
                    isPaused = true;
                    if (countDownTimer != null) countDownTimer.cancel();
                    viewDrumOuter.clearAnimation();
                    btnPauseResume.setText("Resume Cycle");
                    tvDrumPhase.setText("PAUSED");
                    BleManager.getInstance(CycleTimerActivity.this).sendPause();
                    Toast.makeText(CycleTimerActivity.this, "Appliance paused", Toast.LENGTH_SHORT).show();
                } else {
                    isPaused = false;
                    startTimer(millisRemaining);
                    viewDrumOuter.startAnimation(drumAnimation);
                    btnPauseResume.setText("Pause Cycle");
                    BleManager.getInstance(CycleTimerActivity.this).sendResume();
                    Toast.makeText(CycleTimerActivity.this, "Appliance resumed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEmergencyStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer != null) countDownTimer.cancel();
                viewDrumOuter.clearAnimation();
                BleManager.getInstance(CycleTimerActivity.this).sendStop();
                Toast.makeText(CycleTimerActivity.this, "Emergency Stop Triggered! Draining water.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void startTimer(long duration) {
        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisRemaining = millisUntilFinished;
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long sec = seconds % 60;

                tvCountdown.setText(String.format("%02d:%02d", minutes, sec));

                long elapsed = totalDurationMillis - millisUntilFinished;
                int progressPercent = (int) ((elapsed * 100) / totalDurationMillis);
                pbCycleProgress.setProgress(progressPercent);

                // Update stages
                if (progressPercent < 15) {
                    tvDrumPhase.setText("Water Filling");
                    tvStagePercent.setText("Stage 1 of 4: Filling Water (" + progressPercent + "%)");
                    tvCycleDetailSub.setText("Dissolving detergent and balancing laundry weight");
                } else if (progressPercent < 60) {
                    tvDrumPhase.setText("Active Wash Cycle");
                    tvStagePercent.setText("Stage 2 of 4: Main Wash (" + progressPercent + "%)");
                    tvCycleDetailSub.setText("Drum agitation with temperature control");
                } else if (progressPercent < 85) {
                    tvDrumPhase.setText("Rinsing Clothes");
                    tvStagePercent.setText("Stage 3 of 4: Deep Rinse (" + progressPercent + "%)");
                    tvCycleDetailSub.setText("Fresh water intake & foam extraction");
                } else {
                    tvDrumPhase.setText("Spin & Drainage");
                    tvStagePercent.setText("Stage 4 of 4: Final Spin (" + progressPercent + "%)");
                    tvCycleDetailSub.setText("High speed centrifugal extraction: 1000 RPM");
                }
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00:00");
                tvDrumPhase.setText("CYCLE COMPLETED");
                tvStagePercent.setText("Completed (100%)");
                tvCycleDetailSub.setText("Please unload your laundry. Door unlocked.");
                pbCycleProgress.setProgress(100);
                viewDrumOuter.clearAnimation();
                btnPauseResume.setEnabled(false);
                Toast.makeText(CycleTimerActivity.this, "🎉 Wash cycle finished!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
