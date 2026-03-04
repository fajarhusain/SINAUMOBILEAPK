package com.examlock.browser.service;

import android.app.*;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.examlock.browser.R;
import com.examlock.browser.ui.ExamActivity;
import com.examlock.browser.ui.LoginActivity;
import com.examlock.browser.utils.*;
import java.util.*;

public class ExamGuardService extends Service {

    private static final String TAG = "ExamGuardService";
    private static final String CHANNEL_ID = "exam_guard_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int CHECK_INTERVAL_MS = 500; // Check every 500ms

    private Handler guardHandler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private AppPreferences prefs;

    // Allowed packages during exam
    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>(Arrays.asList(
        "com.examlock.browser",
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin", // Keyboard
        "com.samsung.android.honeyboard" // Samsung Keyboard
    ));

    // Floating app packages to block (Extended list)
    private static final Set<String> FLOATING_APP_PACKAGES = new HashSet<>(Arrays.asList(
        // Social Media & Messaging
        "com.facebook.orca",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.telegram.messenger",
        "org.telegram.messenger",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill", // TikTok
        "com.instagram.android",
        "com.twitter.android",
        "com.snapchat.android",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",

        // Browsers & Search
        "com.android.chrome",
        "com.google.android.googlequicksearchbox", // Google App / Gemini
        "com.sec.android.app.sbrowser",
        "com.mi.globalbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx", // Edge
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.UCMobile.intl",
        "com.duckduckgo.mobile.android",

        // AI & Study Tools
        "com.openai.chatgpt",
        "ai.perplexity.app",
        "com.microblink.photomath",
        "co.brainly",
        "com.symbolab.symbolab",
        "com.google.android.apps.bard",

        // Screen Recording & Tools
        "com.miui.screenrecorder",
        "com.android.screenrecorder",
        "net.xmpp.jabber",
        "io.github.subhamthecoder.floatingwindow",
        "com.overlays.android",
        "com.app_essence.floating.apps",
        "com.lwi.android.flapps",
        "com.niftyui.floating.shortcuts"
    ));

    private Runnable guardRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            monitorForegroundApp();
            guardHandler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = AppPreferences.getInstance(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        isRunning = true;
        guardHandler.post(guardRunnable);
        Log.d(TAG, "ExamGuardService started");
        return START_STICKY; // Restart if killed
    }

    private void monitorForegroundApp() {
        if (!prefs.isExamActive()) {
            stopSelf();
            return;
        }

        String foregroundPackage = getForegroundPackage();
        if (foregroundPackage == null) return;

        // Check if package is allowed
        boolean isAllowed = false;
        for (String allowed : ALLOWED_PACKAGES) {
            if (foregroundPackage.startsWith(allowed)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            Log.w(TAG, "SECURITY: Unauthorized app detected: " + foregroundPackage);
            handleUnauthorizedApp(foregroundPackage);
        }

        // Check for floating/overlay apps
        checkForFloatingApps();
    }

    private String getForegroundPackage() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            if (usm == null) return null;

            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            );

            if (appList == null || appList.isEmpty()) return null;

            UsageStats recentStats = null;
            for (UsageStats usageStats : appList) {
                if (recentStats == null ||
                    usageStats.getLastTimeUsed() > recentStats.getLastTimeUsed()) {
                    recentStats = usageStats;
                }
            }

            return recentStats != null ? recentStats.getPackageName() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground package", e);
            return null;
        }
    }

    private void handleUnauthorizedApp(String packageName) {
        // Play warning sound
        SecurityUtils.playWarningAlarm(this, 95);

        // Bring exam app to foreground
        Intent bringFront = new Intent(this, ExamActivity.class);
        bringFront.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                           Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                           Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(bringFront);

        // Reset exam if not returning quickly
        guardHandler.postDelayed(() -> {
            String currentPkg = getForegroundPackage();
            if (currentPkg != null && !currentPkg.equals(getPackageName())) {
                triggerExamReset("App unauthorized: " + packageName);
            }
        }, 3000);
    }

    private void checkForFloatingApps() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am == null) return;

            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes == null) return;

            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (FLOATING_APP_PACKAGES.contains(process.processName)) {
                    Log.w(TAG, "Floating app detected: " + process.processName);
                    SecurityUtils.playWarningAlarm(this, 80);
                    notifyFloatingAppDetected(process.processName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking floating apps", e);
        }
    }

    private void notifyFloatingAppDetected(String packageName) {
        Intent intent = new Intent(ExamActivity.ACTION_EXIT_EXAM);
        intent.putExtra("reason", "floating_app: " + packageName);
        sendBroadcast(intent);
    }

    private void triggerExamReset(String reason) {
        Log.w(TAG, "Triggering exam reset: " + reason);
        prefs.setLoggedIn(false);
        prefs.setExamActive(false);

        SecurityUtils.playWarningAlarm(this, 95);

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SINAU Guard",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("SINAU Security Monitor");
            channel.setShowBadge(false);
            channel.setSound(null, null);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, ExamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔐 SINAU Aktif")
            .setContentText("Ujian sedang berlangsung - Mode pengawasan aktif")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        guardHandler.removeCallbacks(guardRunnable);
        Log.d(TAG, "ExamGuardService destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Restart service if task removed
        Intent restartIntent = new Intent(this, ExamGuardService.class);
        PendingIntent pi = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi);
        }
    }
}
