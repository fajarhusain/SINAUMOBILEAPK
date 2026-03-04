package com.examlock.browser.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.examlock.browser.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRGeneratorActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvQRLabel;
    private Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        setContentView(R.layout.activity_qr_generator);

        ivQRCode = findViewById(R.id.ivQRCode);
        tvQRLabel = findViewById(R.id.tvQRLabel);
        btnClose = findViewById(R.id.btnClose);

        String qrData = getIntent().getStringExtra("qr_data");
        String label = getIntent().getStringExtra("qr_label");

        if (label != null) tvQRLabel.setText(label);
        if (qrData != null) generateQR(qrData);

        btnClose.setOnClickListener(v -> finish());
    }

    private void generateQR(String data) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ?
                        android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR Code", Toast.LENGTH_SHORT).show();
        }
    }
}
