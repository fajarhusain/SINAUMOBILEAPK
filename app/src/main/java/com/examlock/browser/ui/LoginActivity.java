package com.examlock.browser.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.examlock.browser.R;
import com.examlock.browser.service.ExamAccessibilityService;
import com.examlock.browser.utils.SecurityUtils;
import com.examlock.browser.utils.AppPreferences;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnScanQR;
    private TextView tvVersion;
    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen + Secure Flag (no screenshot)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        
        // Immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_login);
        prefs = AppPreferences.getInstance(this);

        initViews();
        setupListeners();
        
        // Initial check for permissions
        checkRequiredPermissions();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnScanQR = findViewById(R.id.btnScanQR);
        tvVersion = findViewById(R.id.tvVersion);

        tvVersion.setText("SINAU v2.0 | Secure Exam Browser");
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (checkRequiredPermissions()) {
                attemptLogin();
            }
        });

        btnScanQR.setOnClickListener(v -> {
            if (checkRequiredPermissions()) {
                Intent intent = new Intent(this, QRScanActivity.class);
                intent.putExtra("mode", "login");
                startActivityForResult(intent, 100);
            }
        });
    }

    private boolean checkRequiredPermissions() {
        boolean accessibilityEnabled = SecurityUtils.isAccessibilityServiceEnabled(this, ExamAccessibilityService.class);
        boolean usageStatsEnabled = SecurityUtils.isUsageStatsPermissionGranted(this);

        if (!accessibilityEnabled || !usageStatsEnabled) {
            showPermissionDialog(accessibilityEnabled, usageStatsEnabled);
            return false;
        }
        return true;
    }

    private void showPermissionDialog(boolean accessibility, boolean usage) {
        StringBuilder message = new StringBuilder("Untuk menggunakan SINAU, Anda harus mengaktifkan izin berikut:\n\n");
        if (!accessibility) message.append("- Accessibility Service (SINAU Security Monitor)\n");
        if (!usage) message.append("- Usage Access (Akses Penggunaan)\n");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Izin Diperlukan")
               .setMessage(message.toString())
               .setCancelable(false)
               .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                   if (!accessibility) {
                       startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                   } else if (!usage) {
                       startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                   }
               })
               .setNegativeButton("Keluar", (dialog, which) -> finishAffinity());
        
        builder.show();
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong!");
            return;
        }

        if (SecurityUtils.isAdminLogin(username, password)) {
            navigateToDashboard(true, username);
            return;
        }

        if (SecurityUtils.isValidStudentLogin(this, username, password)) {
            navigateToDashboard(false, username);
            return;
        }

        showError("Username atau password salah!");
        SecurityUtils.triggerWarningVibration(this);
    }

    private void navigateToDashboard(boolean isAdmin, String username) {
        prefs.setLoggedIn(true);
        prefs.setCurrentUser(username);
        prefs.setIsAdmin(isAdmin);

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("isAdmin", isAdmin);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String qrData = data.getStringExtra("qr_data");
            if (qrData != null) {
                SecurityUtils.parseQRLogin(this, qrData, etUsername, etPassword);
            }
        }
    }

    @Override
    public void onBackPressed() {
        showError("Tidak dapat keluar dari aplikasi ini.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        // Re-check permissions when returning to the app
        checkRequiredPermissions();
    }
}
