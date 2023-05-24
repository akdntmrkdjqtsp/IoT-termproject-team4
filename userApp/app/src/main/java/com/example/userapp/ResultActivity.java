package com.example.userapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "WiFiScanner";

    private WifiManager wifiManager;
    private Timer timer;

    String destination = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        TextView result = findViewById(R.id.result_destination_tv);
        result.setText(getIntent().getStringExtra("destination"));
        destination = getIntent().getStringExtra("destination");

        startTask();
    }

    private void startTask() {
        // 1초마다 작업 실행
        long intervalMillis = 5000;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //작업 실행
                scanWiFiNetworks();
            }
        }, 0, intervalMillis);
    }

    private void stopTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 작업 중지
        stopTask();
    }

    private void scanWiFiNetworks() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
        }
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();
            JsonObject data = new JsonObject();//JsonObject 생성
            String ssid;
            String bssid;
            int rssi;
            for (ScanResult scanResult : scanResults) {
                ssid = scanResult.SSID;
                bssid = scanResult.BSSID;
                rssi = (scanResult.level + 100)*2;

                if(ssid.contains("GC_free_WiFi")){
                    data.addProperty(bssid, rssi);
                }

                Log.d(TAG, "SSID: " + ssid + ", BSSID: " + bssid + ", rssi: " + rssi);
            }

            sendLocationDataToServer(data);
        }
    }

    private void sendLocationDataToServer(JsonObject data) {

        JsonObject requestBody = new JsonObject();
        requestBody.add("signals", data);

        // 서비스 인터페이스 생성
        API apiService = NetworkModule.getRestrofit().create(API.class);
        Call<ResponseBody> call = apiService.sendLocationData(destination, requestBody);

        // Post API 전송
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 요청이 성공적으로 전송된 경우
                    if(response.body() != null) {
                        Log.d("FIND-SUC", response.body().toString());
                    }
                } else {
                    // 요청이 실패한 경우
                    Log.d("FIND-FAIL", "code : " + response.code() + " message : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
                Log.d("FAIL", t.getMessage());
            }
        });
    }
}