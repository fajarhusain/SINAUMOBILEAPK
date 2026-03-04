package com.examlock.browser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.examlock.browser.R;
import com.examlock.browser.utils.AppPreferences;
import com.examlock.browser.utils.SecurityUtils;

public class AdminSettingsActivity extends AppCompatActivity {

    private EditText etInstitution, etExamUrl, etExitPassword, etStudentPassword, etAppPassword;
    private Button btnBlockInternet, btnSave, btnGenerateQR;
    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        setContentView(R.layout.activity_admin_settings);
        prefs = AppPreferences.getInstance(this);
        initViews();
        loadSettings();
    }

    private void initViews() {
        etInstitution = findViewById(R.id.etInstitution);
        etExamUrl = findViewById(R.id.etExamUrl);
        etExitPassword = findViewById(R.id.etExitPassword);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        etAppPassword = findViewById(R.id.etAppPassword);
        btnBlockInternet = findViewById(R.id.swBlockInternet);
        btnSave = findViewById(R.id.btnSave);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);

        btnSave.setOnClickListener(v -> saveSettings());
        btnGenerateQR.setOnClickListener(v -> generateQRCode());
    }

    private void loadSettings() {
        etInstitution.setText(prefs.getInstitutionName());
        etExamUrl.setText(prefs.getExamUrl());
        etExitPassword.setText(prefs.getExitPassword());
        etStudentPassword.setText(prefs.getStudentPassword());
        etAppPassword.setText(prefs.getAppPassword());
    }

    private void saveSettings() {
        prefs.setInstitutionName(etInstitution.getText().toString().trim());
        prefs.setExamUrl(etExamUrl.getText().toString().trim());
        prefs.setExitPassword(etExitPassword.getText().toString().trim());
        prefs.setStudentPassword(etStudentPassword.getText().toString().trim());
        prefs.setAppPassword(etAppPassword.getText().toString().trim());
        Toast.makeText(this, "✅ Pengaturan berhasil disimpan!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void generateQRCode() {
        try {
            String url = etExamUrl.getText().toString().trim();
            String exitPwd = etExitPassword.getText().toString().trim();
            String jsonData = "{\"url\":\"" + url + "\",\"exit_password\":\"" + exitPwd + "\"}";
            String encoded = SecurityUtils.encryptForQR(jsonData);

            Intent intent = new Intent(this, QRGeneratorActivity.class);
            intent.putExtra("qr_data", encoded);
            intent.putExtra("qr_label", "QR Konfigurasi Ujian");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
