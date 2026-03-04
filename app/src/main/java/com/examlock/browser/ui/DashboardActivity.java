package com.examlock.browser.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.examlock.browser.R;
import com.examlock.browser.service.ExamAccessibilityService;
import com.examlock.browser.utils.AppPreferences;
import com.examlock.browser.utils.SecurityUtils;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private EditText etExamUrl, etExitPassword, etStudentName;
    private Switch swOfflineMode;
    private Button btnStartExam, btnScanQR, btnAdminSettings;
    private TextView tvWelcome, tvAppStatus;
    private LinearLayout layoutAdminDashboard, layoutAdminPanel, layoutStudentView;
    private View cardConfig, cardGoToExam;
    private AppPreferences prefs;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setImmersive();
        setContentView(R.layout.activity_dashboard);

        prefs = AppPreferences.getInstance(this);
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        initViews();
        setupDashboardMode();
        loadSavedSettings();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        isAdmin = intent.getBooleanExtra("isAdmin", false);
        setupDashboardMode();
        loadSavedSettings();
    }

    private void initViews() {
        etExamUrl = findViewById(R.id.etExamUrl);
        etExitPassword = findViewById(R.id.etExitPassword);
        etStudentName = findViewById(R.id.etStudentName);
        swOfflineMode = findViewById(R.id.swOfflineMode);
        btnStartExam = findViewById(R.id.btnStartExam);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnAdminSettings = findViewById(R.id.btnAdminSettings);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAppStatus = findViewById(R.id.tvAppStatus);
        
        layoutAdminDashboard = findViewById(R.id.layoutAdminDashboard);
        layoutAdminPanel = findViewById(R.id.layoutAdminPanel);
        layoutStudentView = findViewById(R.id.layoutStudentView);
        cardConfig = findViewById(R.id.cardConfig);
        cardGoToExam = findViewById(R.id.cardGoToExam);

        tvWelcome.setText(isAdmin ? "Dashboard Admin" : "Dashboard Siswa");
        tvAppStatus.setText("SINAU v2.0");

        // Action for Admin Dashboard Buttons
        cardConfig.setOnClickListener(v -> {
            if (layoutAdminPanel.getVisibility() == View.VISIBLE) {
                layoutAdminPanel.setVisibility(View.GONE);
            } else {
                layoutAdminPanel.setVisibility(View.VISIBLE);
            }
        });

        cardGoToExam.setOnClickListener(v -> {
            layoutStudentView.setVisibility(View.VISIBLE);
            // Auto scroll to bottom
            findViewById(R.id.layoutStudentView).requestFocus();
        });

        btnStartExam.setOnClickListener(v -> startExam());

        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanActivity.class);
            intent.putExtra("mode", "exam_setup");
            startActivityForResult(intent, 200);
        });

        btnAdminSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminSettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupDashboardMode() {
        if (isAdmin) {
            layoutAdminDashboard.setVisibility(View.VISIBLE);
            layoutStudentView.setVisibility(View.GONE); // Hidden initially, shown via "Go to Exam"
            layoutAdminPanel.setVisibility(View.GONE);
            tvWelcome.setText("Dashboard Admin");
        } else {
            layoutAdminDashboard.setVisibility(View.GONE);
            layoutAdminPanel.setVisibility(View.GONE);
            layoutStudentView.setVisibility(View.VISIBLE);
            tvWelcome.setText("Dashboard Siswa");
        }
    }

    private void loadSavedSettings() {
        etExamUrl.setText(prefs.getExamUrl());
        etExitPassword.setText(prefs.getExitPassword());
        etStudentName.setText(prefs.getCurrentUser());
    }

    private void startExam() {
        String studentNameInput = etStudentName.getText().toString().trim();

        // Updated Logic: If student name is "admin", go to LoginActivity first
        if (!isAdmin && "admin".equalsIgnoreCase(studentNameInput)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            // Optional: reset the field so it doesn't trigger again immediately on return
            etStudentName.setText("");
            return;
        }

        if (!SecurityUtils.isAccessibilityServiceEnabled(this, ExamAccessibilityService.class)) {
            showAccessibilityDialog();
            return;
        }

        String examUrl = etExamUrl.getText().toString().trim();
        String exitPwd = etExitPassword.getText().toString().trim();
        String studentName = etStudentName.getText().toString().trim();
        boolean offlineMode = swOfflineMode != null && swOfflineMode.isChecked();

        if (!offlineMode && examUrl.isEmpty()) {
            Toast.makeText(this, "Masukkan URL ujian atau aktifkan mode offline", Toast.LENGTH_SHORT).show();
            return;
        }

        if (exitPwd.isEmpty()) {
            Toast.makeText(this, "Password keluar tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (studentName.isEmpty()) {
            Toast.makeText(this, "Nama siswa tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save settings
        prefs.setExamUrl(examUrl);
        prefs.setExitPassword(exitPwd);
        prefs.setCurrentUser(studentName);
        prefs.setExamActive(true);

        Intent intent = new Intent(this, ExamActivity.class);
        intent.putExtra("exam_url", offlineMode ? "" : examUrl);
        intent.putExtra("exit_password", exitPwd);
        intent.putExtra("student_name", studentName);
        intent.putExtra("offline_mode", offlineMode);
        startActivity(intent);
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Aktifkan Accessibility Service untuk keamanan maksimal.")
            .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            String qrData = data.getStringExtra("qr_data");
            if (qrData != null) {
                SecurityUtils.parseQRExamSetup(this, qrData, etExamUrl, etExitPassword, etStudentName);
            }
        }
    }

    private void setImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    @Override
    public void onBackPressed() {
        if (isAdmin && layoutStudentView.getVisibility() == View.VISIBLE) {
            layoutStudentView.setVisibility(View.GONE);
            return;
        }
        if (isAdmin && layoutAdminPanel.getVisibility() == View.VISIBLE) {
            layoutAdminPanel.setVisibility(View.GONE);
            return;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersive();
    }
}
