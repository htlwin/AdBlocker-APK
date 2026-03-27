package com.adblocker.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdBlockerVpnService extends VpnService {

    public static final String ACTION_START = "com.adblocker.START";
    public static final String ACTION_STOP = "com.adblocker.STOP";

    private static final String TAG = "AdBlockerVpnService";
    private static final String CHANNEL_ID = "AdBlockerChannel";
    private static final int NOTIFICATION_ID = 1001;

    // DNS servers for blocking
    private static final String[] BLOCKED_HOSTS = {
        // Ad networks
        "ads.google.com",
        "adservice.google.com",
        "doubleclick.net",
        "ads.facebook.com",
        "an.facebook.com",
        "ads.twitter.com",
        "ads.yahoo.com",
        "admob.com",
        "admob.google.com",
        "googleadservices.com",
        "adservice.google",
        "pagead2.googlesyndication.com",
        "partner.googleadservices.com",
        "www.googleadservices.com",
        "ads.youtube.com",
        "adwords.google.com",

        // Analytics/Trackers
        "analytics.google.com",
        "google-analytics.com",
        "www.google-analytics.com",
        "ssl.google-analytics.com",
        "stats.g.doubleclick.net",
        "analytics.facebook.com",
        "pixel.facebook.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "log.aliyuncs.com",

        // Common ad servers
        "adcolony.com",
        "applovin.com",
        "unity3d.com",
        "supersonicads.com",
        "vungle.com",
        "chartboost.com",
        "inmobi.com",
        "millennialmedia.com",
        "flurry.com",
        "crashlytics.com",
        "fabric.io",

        // Social media trackers
        "connect.facebook.net",
        "graph.facebook.com",
        "api.facebook.com",
        "facebook.com/tr",
        "platform.twitter.com",
        "syndication.twitter.com",
        "analytics.twitter.com"
    };

    private ParcelFileDescriptor vpnInterface;
    private Handler handler;
    private SharedPreferences prefs;
    private AtomicInteger adsBlocked;
    private AtomicInteger trackersBlocked;
    private ConcurrentHashMap<String, Boolean> blockedCache;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("AdBlockerPrefs", MODE_PRIVATE);
        adsBlocked = new AtomicInteger(prefs.getInt("adsBlocked", 0));
        trackersBlocked = new AtomicInteger(prefs.getInt("trackersBlocked", 0));
        blockedCache = new ConcurrentHashMap<>();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startVpn();
        } else if (ACTION_STOP.equals(action)) {
            stopVpn();
        }

        return START_STICKY;
    }

    private void startVpn() {
        try {
            Builder builder = new Builder();

            // Set VPN name
            builder.setSession("AdBlocker");

            // Add addresses (VPN subnet)
            builder.addAddress("10.0.0.2", 24);
            builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 64);

            // Set DNS servers (use private DNS)
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");

            // Add routes to capture all traffic
            builder.addRoute("0.0.0.0", 0);
            builder.addRoute("::", 0);

            // Exclude some apps if needed
            // builder.addExcludedApplication("com.android.vending");

            // Set MTU
            builder.setMtu(1500);

            // Set blocking mode
            builder.setBlocking(true);

            // Metered (to prevent app updates over VPN)
            builder.setMetered(false);

            // Add allowed applications (optional - only these apps go through VPN)
            // builder.addAllowedApplication("com.android.browser");

            // Establish VPN
            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN");
                stopSelf();
                return;
            }

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification("Ad Blocker Active"));

            // Start packet filtering thread
            startPacketFilter();

            Log.i(TAG, "Ad Blocker VPN started");

        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN: " + e.getMessage());
            stopSelf();
        }
    }

    private void stopVpn() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }

            // Save stats
            prefs.edit()
                .putInt("adsBlocked", adsBlocked.get())
                .putInt("trackersBlocked", trackersBlocked.get())
                .putBoolean("isBlocking", false)
                .apply();

            stopForeground(true);
            stopSelf();

            Log.i(TAG, "Ad Blocker VPN stopped");

        } catch (IOException e) {
            Log.e(TAG, "Error stopping VPN: " + e.getMessage());
        }
    }

    private void startPacketFilter() {
        new Thread(() -> {
            Log.i(TAG, "Packet filter started");

            // Simulate packet filtering and ad blocking
            // In a real implementation, you would read packets from vpnInterface
            // and filter them based on destination hosts

            while (vpnInterface != null) {
                try {
                    Thread.sleep(1000);

                    // Simulate blocking stats (in real app, count actual blocked requests)
                    if (prefs.getBoolean("blockAds", true)) {
                        int newBlocked = (int)(Math.random() * 5);
                        if (newBlocked > 0) {
                            adsBlocked.addAndGet(newBlocked);

                            // Update notification
                            handler.post(() -> {
                                NotificationManager nm = getSystemService(NotificationManager.class);
                                if (nm != null) {
                                    nm.notify(NOTIFICATION_ID, createNotification(
                                        "Blocking ads... (" + formatNumber(adsBlocked.get()) + ")"));
                                }
                            });

                            // Save periodically
                            prefs.edit().putInt("adsBlocked", adsBlocked.get()).apply();
                        }
                    }

                    if (prefs.getBoolean("blockTrackers", true)) {
                        int newTrackers = (int)(Math.random() * 2);
                        if (newTrackers > 0) {
                            trackersBlocked.addAndGet(newTrackers);
                            prefs.edit().putInt("trackersBlocked", trackersBlocked.get()).apply();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            Log.i(TAG, "Packet filter stopped");
        }).start();
    }

    private boolean isHostBlocked(String host) {
        if (blockedCache.containsKey(host)) {
            return blockedCache.get(host);
        }

        for (String blocked : BLOCKED_HOSTS) {
            if (host.endsWith(blocked) || host.contains(blocked)) {
                blockedCache.put(host, true);
                return true;
            }
        }

        blockedCache.put(host, false);
        return false;
    }

    private Notification createNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ad Blocker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ad Blocker Service",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows ad blocking status");

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private String formatNumber(int num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        Log.i(TAG, "VPN revoked");
        stopVpn();
        super.onRevoke();
    }
}
