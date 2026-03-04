package com.examlock.browser.utils;

import android.accessibilityservice.AccessibilityService;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.media.*;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.EditText;
import android.util.Log;
import org.json.JSONObject;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD_HASH = "5f4dcc3b5aa765d61d8327deb882cf99"; // "password" md5 - change this!

    // Default student credentials (in real app, load from server or config)
    private static final String STUDENT_PASSWORD = "siswa2024";

    public static boolean isAdminLogin(String username, String password) {
        return ADMIN_USERNAME.equals(username) && md5(password).equals(ADMIN_PASSWORD_HASH);
    }

    public static boolean isValidStudentLogin(Context context, String username, String password) {
        AppPreferences prefs = AppPreferences.getInstance(context);
        String savedPwd = prefs.getStudentPassword();
        if (savedPwd == null || savedPwd.isEmpty()) {
            savedPwd = STUDENT_PASSWORD;
        }
        return !username.isEmpty() && password.equals(savedPwd);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        int accessibilityEnabled = 0;
        final String serviceName = context.getPackageName() + "/" + service.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding accessibility settings", e);
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(serviceName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isUsageStatsPermissionGranted(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void playWarningAlarm(Context context, int volumePercent) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                int targetVol = (int)(maxVol * (volumePercent / 100.0f));
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0);
            }

            MediaPlayer mp = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
            if (mp != null) {
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            } else {
                // Fallback to tone
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM,
                    volumePercent);
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 3000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing warning alarm", e);
        }
    }

    public static void triggerWarningVibration(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long[] pattern = {0, 500, 200, 500, 200, 500};
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                    vibrator.vibrate(effect);
                } else {
                    long[] pattern = {0, 500, 200, 500};
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering vibration", e);
        }
    }

    public static boolean isAllowedUrl(String baseUrl, String targetUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return true;
        if (targetUrl == null || targetUrl.isEmpty()) return false;

        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL target = new java.net.URL(targetUrl);
            String baseHost = base.getHost();
            String targetHost = target.getHost();
            return baseHost != null && baseHost.equals(targetHost);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse QR code data for login
     * QR format (encrypted base64 JSON): {"username":"xxx","password":"xxx"}
     */
    public static void parseQRLogin(Context context, String qrData, EditText etUsername, EditText etPassword) {
        try {
            // Try decrypting
            String decoded = decryptQRData(qrData);
            JSONObject json = new JSONObject(decoded);
            if (json.has("username")) etUsername.setText(json.getString("username"));
            if (json.has("password")) etPassword.setText(json.getString("password"));
        } catch (Exception e) {
            // Try raw JSON
            try {
                JSONObject json = new JSONObject(qrData);
                if (json.has("username")) etUsername.setText(json.getString("username"));
                if (json.has("password")) etPassword.setText(json.getString("password"));
            } catch (Exception e2) {
                Log.e(TAG, "Error parsing QR login data", e2);
            }
        }
    }

    /**
     * Parse QR code data for exam setup
     * QR format: {"url":"xxx","exit_password":"xxx","student":"xxx"}
     */
    public static void parseQRExamSetup(Context context, String qrData,
                                         EditText etUrl, EditText etExitPwd, EditText etStudent) {
        try {
            String decoded = decryptQRData(qrData);
            JSONObject json = new JSONObject(decoded);
            if (json.has("url") && etUrl != null) etUrl.setText(json.getString("url"));
            if (json.has("exit_password") && etExitPwd != null) etExitPwd.setText(json.getString("exit_password"));
            if (json.has("student") && etStudent != null) etStudent.setText(json.getString("student"));
        } catch (Exception e) {
            try {
                JSONObject json = new JSONObject(qrData);
                if (json.has("url") && etUrl != null) etUrl.setText(json.getString("url"));
                if (json.has("exit_password") && etExitPwd != null) etExitPwd.setText(json.getString("exit_password"));
                if (json.has("student") && etStudent != null) etStudent.setText(json.getString("student"));
            } catch (Exception e2) {
                Log.e(TAG, "Error parsing QR exam setup", e2);
            }
        }
    }

    private static String decryptQRData(String data) {
        try {
            // Simple Base64 decode (in production use AES encryption)
            byte[] decoded = Base64.decode(data, Base64.DEFAULT);
            return new String(decoded);
        } catch (Exception e) {
            return data;
        }
    }

    /**
     * Encrypt data for QR generation
     */
    public static String encryptForQR(String data) {
        return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
    }

    private static String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
