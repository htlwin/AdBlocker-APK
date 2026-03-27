package com.adblocker.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            SharedPreferences prefs = context.getSharedPreferences("AdBlockerPrefs", Context.MODE_PRIVATE);
            boolean wasBlocking = prefs.getBoolean("isBlocking", false);

            if (wasBlocking) {
                // Start the VPN service on boot if it was active before
                Intent serviceIntent = new Intent(context, AdBlockerVpnService.class);
                serviceIntent.setAction(AdBlockerVpnService.ACTION_START);

                try {
                    context.startForegroundService(serviceIntent);
                } catch (Exception e) {
                    // Service might fail to start on some devices
                }
            }
        }
    }
}
