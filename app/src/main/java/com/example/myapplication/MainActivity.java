package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; // Tag for logging
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private EditText ipInput;
    private Button scanButton;
    private TextView qrResult;
    private TextView ipTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipInput = findViewById(R.id.ipInput);
        scanButton = findViewById(R.id.scanButton);
        qrResult = findViewById(R.id.qrResult);
        ipTextView = findViewById(R.id.iptext);

        scanButton.setOnClickListener(v -> {
            // Start QR code scanning
            Log.d(TAG, "Scan button clicked");
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a QR code");
            integrator.setCameraId(0); // Use a specific camera of the device
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                qrResult.setText("Cancelled");
                Log.d(TAG, "QR Code scan cancelled");
            } else {
                qrResult.setText("Scanned: " + result.getContents());
                Log.d(TAG, "QR Code scanned: " + result.getContents());
                String serverIp = ipInput.getText().toString(); // Get IP from EditText
                sendQRCodeToServer(result.getContents(), serverIp);
            }
        }
    }

    private void sendQRCodeToServer(String qrCode, String serverIp) {
        Log.d(TAG, "Sending QR code to server: " + qrCode + " at IP: " + serverIp);
        executorService.execute(() -> {
            try {
                // Construct the URL using the serverIp provided by the user
                URL url = new URL("http://" + serverIp + ":8080");
                Log.d(TAG, "Connecting to URL: " + url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("qr_code", qrCode);

                Log.d(TAG, "Writing QR code to output stream");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonParam.toString().getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Data sent successfully.");
                } else {
                    Log.d(TAG, "Failed to send data. Response Code: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error while sending QR code: " + e.getMessage(), e);
            }
        });
    }
}
