package com.adblocker.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 100;

    private Button btnToggle;
    private ImageView ivStatus;
    private TextView tvStatus, tvAdsBlocked;
    private SharedPreferences prefs;

    private boolean isBlocking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AdBlockerPrefs", MODE_PRIVATE);
        isBlocking = prefs.getBoolean("isBlocking", false);

        initViews();
        updateUI();
    }

    private void initViews() {
        btnToggle = findViewById(R.id.btnToggle);
        ivStatus = findViewById(R.id.ivStatus);
        tvStatus = findViewById(R.id.tvStatus);
        tvAdsBlocked = findViewById(R.id.tvAdsBlocked);

        btnToggle.setOnClickListener(v -> toggleAdBlocker());

        // Stats button
        findViewById(R.id.btnStats).setOnClickListener(v -> showStats());

        // Settings button
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());

        // About button
        findViewById(R.id.btnAbout).setOnClickListener(v -> showAbout());
    }

    private void updateUI() {
        if (isBlocking) {
            ivStatus.setImageResource(R.drawable.ic_shield_active);
            tvStatus.setText("Ad Blocker Active");
            tvStatus.setTextColor(getColor(R.color.green));
            btnToggle.setText("STOP");
            btnToggle.setBackgroundColor(getColor(R.color.red));
        } else {
            ivStatus.setImageResource(R.drawable.ic_shield_inactive);
            tvStatus.setText("Ad Blocker Inactive");
            tvStatus.setTextColor(getColor(R.color.gray));
            btnToggle.setText("START");
            btnToggle.setBackgroundColor(getColor(R.color.green));
        }

        int blockedCount = prefs.getInt("adsBlocked", 0);
        tvAdsBlocked.setText("Ads Blocked: " + formatNumber(blockedCount));
    }

    private void toggleAdBlocker() {
        if (isBlocking) {
            stopAdBlocker();
        } else {
            startAdBlocker();
        }
    }

    private void startAdBlocker() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, AdBlockerVpnService.class);
            serviceIntent.setAction(AdBlockerVpnService.ACTION_START);
            startForegroundService(serviceIntent);
            setBlocking(true);
        }
    }

    private void stopAdBlocker() {
        Intent serviceIntent = new Intent(this, AdBlockerVpnService.class);
        serviceIntent.setAction(AdBlockerVpnService.ACTION_STOP);
        startService(serviceIntent);
        setBlocking(false);
    }

    private void setBlocking(boolean blocking) {
        isBlocking = blocking;
        prefs.edit().putBoolean("isBlocking", blocking).apply();
        updateUI();

        String msg = blocking ? "Ad Blocker Started" : "Ad Blocker Stopped";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showStats() {
        int blocked = prefs.getInt("adsBlocked", 0);
        long dataSaved = prefs.getLong("dataSaved", 0);

        String stats = "📊 Statistics\n\n" +
                "Ads Blocked: " + formatNumber(blocked) + "\n" +
                "Data Saved: " + formatDataSize(dataSaved) + "\n" +
                "Trackers Blocked: " + formatNumber(prefs.getInt("trackersBlocked", 0));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Statistics")
                .setMessage(stats)
                .setPositiveButton("Reset", (d, w) -> {
                    prefs.edit()
                        .putInt("adsBlocked", 0)
                        .putLong("dataSaved", 0)
                        .putInt("trackersBlocked", 0)
                        .apply();
                    updateUI();
                    Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSettings() {
        String[] options = {"Block Ads", "Block Trackers", "Block Social Media", "Custom Filters"};
        boolean[] checked = {
            prefs.getBoolean("blockAds", true),
            prefs.getBoolean("blockTrackers", true),
            prefs.getBoolean("blockSocial", false),
            prefs.getBoolean("customFilters", false)
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Settings")
                .setMultiChoiceItems(options, checked, (d, which, isChecked) -> {
                    switch (which) {
                        case 0: prefs.edit().putBoolean("blockAds", isChecked).apply(); break;
                        case 1: prefs.edit().putBoolean("blockTrackers", isChecked).apply(); break;
                        case 2: prefs.edit().putBoolean("blockSocial", isChecked).apply(); break;
                        case 3: prefs.edit().putBoolean("customFilters", isChecked).apply(); break;
                    }
                })
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAbout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About Ad Blocker")
                .setMessage("Ad Blocker v1.0\n\n" +
                        "A free and open-source ad blocker for Android.\n\n" +
                        "Features:\n" +
                        "• Blocks ads in apps and browsers\n" +
                        "• Blocks trackers and analytics\n" +
                        "• Saves data and battery\n" +
                        "• No root required\n\n" +
                        "Uses local VPN to filter traffic.")
                .setPositiveButton("OK", null)
                .setNeutralButton("GitHub", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/htlwin/AdBlocker-APK")));
                    } catch (Exception e) {}
                })
                .show();
    }

    private String formatNumber(int num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
    }

    private String formatDataSize(long bytes) {
        if (bytes >= 1073741824) {
            return String.format("%.2f GB", bytes / 1073741824.0);
        } else if (bytes >= 1048576) {
            return String.format("%.2f MB", bytes / 1048576.0);
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBlocking = prefs.getBoolean("isBlocking", false);
        updateUI();
    }
}
