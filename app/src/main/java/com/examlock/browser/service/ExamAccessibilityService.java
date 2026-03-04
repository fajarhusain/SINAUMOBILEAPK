package com.examlock.browser.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import com.examlock.browser.ui.ExamActivity;
import com.examlock.browser.utils.*;
import java.util.*;

public class ExamAccessibilityService extends AccessibilityService {

    private static final String TAG = "ExamAccessibility";
    private AppPreferences prefs;
    private long lastWarningTime = 0;

    private static final Set<String> BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
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
        
        // Browsers & Search (Blocks ChatGPT/Gemini web)
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
        "com.google.android.apps.bard", // Gemini (if applicable)

        // System & Utilities
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.miui.screenrecorder",
        "com.android.screenrecorder",
        "com.samsung.android.game.gamehome",
        "com.samsung.android.game.gametools",
        "com.miui.bubbles",
        "com.android.systemui.bubbles",
        "com.app_essence.floating.apps",
        "com.lwi.android.flapps",
        "com.niftyui.floating.shortcuts"
    ));

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = AppPreferences.getInstance(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);

        Log.d(TAG, "ExamAccessibilityService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!prefs.isExamActive()) return;

        int eventType = event.getEventType();
        CharSequence packageName = event.getPackageName();

        if (packageName == null) return;
        String pkg = packageName.toString();

        // Check if package is the app itself, don't block
        if (pkg.equals(getPackageName())) return;

        // 1. Detect Dual Screen / Split Screen
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            checkMultiWindowMode();
        }

        // 2. Detect Blocked App / Floating Apps
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (BLOCKED_PACKAGES.contains(pkg)) {
                Log.w(TAG, "BLOCKED app detected in accessibility: " + pkg);
                showSecurityToast("⚠️ APLIKASI TERLARANG TERDETEKSI: " + pkg);
                handleBlockedApp(pkg);
            }

            // Detect System UI (Notifications, Status Bar, Quick Settings)
            if (pkg.equals("com.android.systemui")) {
                CharSequence className = event.getClassName();
                if (className != null) {
                    String cls = className.toString();
                    if (cls.contains("NotificationShade") ||
                        cls.contains("StatusBar") ||
                        cls.contains("QuickSettings") ||
                        cls.contains("SplitScreen")) {
                        
                        Log.w(TAG, "System UI intervention detected - blocking");
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        SecurityUtils.playWarningAlarm(this, 70);
                        showSecurityToast("⚠️ DILARANG MEMBUKA PANEL SISTEM!");
                    }
                }
            }
        }
    }

    private void checkMultiWindowMode() {
        // Implementation for detecting multi-window mode can be added here
    }

    private void handleBlockedApp(String packageName) {
        // Force back to exam
        performGlobalAction(GLOBAL_ACTION_BACK);

        // Trigger exam app to front
        Intent intent = new Intent(this, ExamActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                       Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);

        SecurityUtils.playWarningAlarm(this, 95);
        SecurityUtils.triggerWarningVibration(this);
        Log.w(TAG, "Blocked and redirected from: " + packageName);
    }

    private void showSecurityToast(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarningTime > 3000) { // Throttle toast
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            });
            lastWarningTime = currentTime;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "ExamAccessibilityService interrupted");
    }
}
