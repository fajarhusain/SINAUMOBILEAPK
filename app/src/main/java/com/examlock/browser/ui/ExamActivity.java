package com.examlock.browser.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.examlock.browser.R;
import com.examlock.browser.service.ExamGuardService;
import com.examlock.browser.utils.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExamActivity extends AppCompatActivity {

    private static final String TAG = "ExamActivity";
    public static final String ACTION_EXIT_EXAM = "com.examlock.browser.EXIT_EXAM";

    private WebView examWebView;
    private TextView tvTime, tvBattery, tvStudentName, tvStatus;
    private ImageView ivBtStatus, ivHeadsetStatus, ivWifiStatus;
    private ImageButton btnRefresh;
    private LinearLayout llStatusBar, offlineView;
    private FrameLayout loadingOverlay;
    private Button btnRetryConnection;

    private String examUrl;
    private String studentName;
    private String exitPassword;
    private boolean isExamActive = false;
    private boolean isExiting = false;

    private Handler clockHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer warningPlayer;
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver bluetoothReceiver;
    private BroadcastReceiver headsetReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;

    // Runnable to update clock
    private Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== SECURITY FLAGS =====
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        setImmersiveMode();
        setContentView(R.layout.activity_exam);

        // Get data from intent
        Intent intent = getIntent();
        examUrl = intent.getStringExtra("exam_url");
        studentName = intent.getStringExtra("student_name");
        exitPassword = intent.getStringExtra("exit_password");
        if (exitPassword == null || exitPassword.isEmpty()) {
            exitPassword = AppPreferences.getInstance(this).getExitPassword();
        }

        initViews();
        setupWebView();
        setupReceivers();
        setupNetworkMonitoring();
        startGuardService();
        startClock();
        loadExam();

        isExamActive = true;
    }

    private void initViews() {
        examWebView = findViewById(R.id.examWebView);
        tvTime = findViewById(R.id.tvTime);
        tvBattery = findViewById(R.id.tvBattery);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStatus = findViewById(R.id.tvStatus);
        ivBtStatus = findViewById(R.id.ivBtStatus);
        ivHeadsetStatus = findViewById(R.id.ivHeadsetStatus);
        ivWifiStatus = findViewById(R.id.ivWifiStatus);
        btnRefresh = findViewById(R.id.btnRefresh);
        llStatusBar = findViewById(R.id.llStatusBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        offlineView = findViewById(R.id.offlineView);
        btnRetryConnection = findViewById(R.id.btnRetryConnection);

        tvStudentName.setText("Siswa: " + (studentName != null ? studentName : "Unknown"));

        btnRefresh.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                examWebView.reload();
                Toast.makeText(this, "Memuat ulang halaman...", Toast.LENGTH_SHORT).show();
            } else {
                showOfflineMessage();
            }
        });

        btnRetryConnection.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                loadExam();
            } else {
                Toast.makeText(this, "Internet masih terputus!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = examWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Inject anti-copy-paste JavaScript
        examWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (offlineView.getVisibility() != View.VISIBLE) {
                    injectAntiCheatJS(view);
                    loadingOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request.getUrl().toString();
                // Only allow same domain or specific allowed domains
                if (SecurityUtils.isAllowedUrl(examUrl, targetUrl)) {
                    return false;
                }
                return true; // Block other URLs
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // For main frame errors, show offline message
                if (request.isForMainFrame()) {
                    showOfflineMessage();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Legacy error handling
                showOfflineMessage();
            }
        });

        examWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return true; // Suppress console
            }
        });

        // Disable long press (anti copy-paste)
        examWebView.setOnLongClickListener(v -> true);
        examWebView.setLongClickable(false);

        // Disable text selection
        examWebView.setOnTouchListener((v, event) -> {
            if (event.getPointerCount() > 1) return true; // Block multi-touch
            return false;
        });
    }

    private void injectAntiCheatJS(WebView view) {
        String js = "javascript:" +
            // Disable right click
            "document.addEventListener('contextmenu', function(e){ e.preventDefault(); return false; });" +
            // Disable copy
            "document.addEventListener('copy', function(e){ e.preventDefault(); return false; });" +
            // Disable cut
            "document.addEventListener('cut', function(e){ e.preventDefault(); return false; });" +
            // Disable paste
            "document.addEventListener('paste', function(e){ e.preventDefault(); return false; });" +
            // Disable select all
            "document.addEventListener('selectstart', function(e){ e.preventDefault(); return false; });" +
            // Disable print
            "window.onbeforeprint = function(){ return false; };" +
            // Disable drag
            "document.addEventListener('dragstart', function(e){ e.preventDefault(); return false; });" +
            // Apply no-select CSS
            "var style = document.createElement('style');" +
            "style.innerHTML = '* { -webkit-user-select: none !important; user-select: none !important; -webkit-touch-callout: none !important; }';" +
            "document.head.appendChild(style);" +
            // Detect dev tools (basic)
            "setInterval(function(){ var d = new Date(); var t1 = d.getTime(); debugger; var t2 = new Date().getTime(); if(t2-t1 > 100){ window.location.reload(); } }, 3000);";

        view.evaluateJavascript(js, null);
    }

    private void setupReceivers() {
        // Battery receiver
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int)((level / (float) scale) * 100);
                tvBattery.setText("🔋 " + batteryPct + "%");
                if (batteryPct <= 15) {
                    tvBattery.setTextColor(Color.RED);
                } else {
                    tvBattery.setTextColor(Color.WHITE);
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Bluetooth receiver
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkBluetooth();
            }
        };
        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        btFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, btFilter);

        // Headset receiver
        headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra("state", -1);
                boolean connected = state == 1;
                updateHeadsetStatus(connected);
                if (connected) {
                    showSecurityWarning("⚠️ PERINGATAN: Headset terdeteksi! Harap lepas headset.");
                }
            }
        };
        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    runOnUiThread(() -> {
                        ivWifiStatus.setColorFilter(Color.WHITE);
                        if (offlineView.getVisibility() == View.VISIBLE) {
                            Toast.makeText(ExamActivity.this, "Internet terhubung kembali!", Toast.LENGTH_SHORT).show();
                            loadExam();
                        }
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    runOnUiThread(() -> {
                        ivWifiStatus.setColorFilter(Color.RED);
                        showOfflineMessage();
                    });
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                    super.onCapabilitiesChanged(network, capabilities);
                    boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    runOnUiThread(() -> {
                        if (!hasInternet) {
                            ivWifiStatus.setColorFilter(Color.RED);
                            showOfflineMessage();
                        } else {
                            ivWifiStatus.setColorFilter(Color.WHITE);
                        }
                    });
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    private void checkBluetooth() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean btEnabled = btAdapter != null && btAdapter.isEnabled();
        ivBtStatus.setColorFilter(btEnabled ? Color.YELLOW : Color.GRAY);
        if (btEnabled) {
            showSecurityWarning("⚠️ PERINGATAN: Bluetooth aktif! Harap matikan Bluetooth.");
        }
    }

    private void updateHeadsetStatus(boolean connected) {
        ivHeadsetStatus.setColorFilter(connected ? Color.YELLOW : Color.GRAY);
    }

    private void loadExam() {
        if (!isNetworkAvailable()) {
            showOfflineMessage();
            return;
        }

        runOnUiThread(() -> {
            offlineView.setVisibility(View.GONE);
            examWebView.setVisibility(View.VISIBLE);
            loadingOverlay.setVisibility(View.VISIBLE);
            tvStatus.setText("● UJIAN AKTIF");
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        });

        if (examUrl != null && !examUrl.isEmpty()) {
            examWebView.loadUrl(examUrl);
        } else {
            examWebView.loadUrl("about:blank");
            examWebView.loadData(getOfflineExamHtml(), "text/html", "UTF-8");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                                   actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                                   actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || 
                                   actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }

    private String getOfflineExamHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<title>Ujian Offline</title></head><body style='font-family:Arial;text-align:center;padding:20px;'>" +
               "<h2>Mode Ujian Offline</h2><p>Soal ujian akan dimuat oleh pengawas.</p>" +
               "</body></html>";
    }

    private void showOfflineMessage() {
        runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.GONE);
            examWebView.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
            tvStatus.setText("⚠️ KONEKSI TERPUTUS");
            tvStatus.setTextColor(Color.RED);
        });
    }

    private void startGuardService() {
        Intent serviceIntent = new Intent(this, ExamGuardService.class);
        serviceIntent.putExtra("exam_active", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void startClock() {
        clockHandler.post(clockRunnable);
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        tvTime.setText("🕐 " + sdf.format(new Date()));
    }

    private void setImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSecurityWarning(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            SecurityUtils.triggerWarningVibration(this);
        });
    }

    // ===== EXIT EXAM WITH PASSWORD =====
    public void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ExamDialogTheme);
        builder.setTitle("🔐 Keluar Ujian");
        builder.setMessage("Masukkan password untuk keluar dari ujian:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                          android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password exit");
        builder.setView(input);

        builder.setPositiveButton("Keluar", (dialog, which) -> {
            String enteredPassword = input.getText().toString();
            if (enteredPassword.equals(exitPassword)) {
                performExitExam();
            } else {
                showSecurityWarning("❌ Password salah! Tidak dapat keluar.");
                SecurityUtils.playWarningAlarm(ExamActivity.this, 95);
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            );
        }
        dialog.show();
    }

    private void performExitExam() {
        isExiting = true;
        isExamActive = false;

        // Stop guard service
        stopService(new Intent(this, ExamGuardService.class));

        // Reset exam state
        AppPreferences prefs = AppPreferences.getInstance(this);
        prefs.setExamActive(false);
        prefs.setLoggedIn(false);

        // Navigate back to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ===== BLOCK NAVIGATION =====
    @Override
    public void onBackPressed() {
        if (isExamActive && !isExiting) {
            showExitDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isExamActive && !isExiting) {
            // Play warning alarm when leaving exam
            SecurityUtils.playWarningAlarm(this, 95);
            showWarningAndReturn();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isExamActive && !isExiting) {
            SecurityUtils.playWarningAlarm(this, 95);
        }
    }

    private void showWarningAndReturn() {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> {
            if (!isExiting && isExamActive) {
                // Reset exam - go back to login
                AppPreferences prefs = AppPreferences.getInstance(this);
                prefs.setLoggedIn(false);
                prefs.setExamActive(false);

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveMode();
        checkBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);

        if (batteryReceiver != null) {
            try { unregisterReceiver(batteryReceiver); } catch (Exception e) {}
        }
        if (bluetoothReceiver != null) {
            try { unregisterReceiver(bluetoothReceiver); } catch (Exception e) {}
        }
        if (headsetReceiver != null) {
            try { unregisterReceiver(headsetReceiver); } catch (Exception e) {}
        }
        if (networkCallback != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {}
        }
        if (warningPlayer != null) {
            warningPlayer.release();
        }
        if (examWebView != null) {
            examWebView.destroy();
        }
    }
}
