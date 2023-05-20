package com.example.userapp;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "WiFiScanner";

    private WifiManager wifiManager;
    private Timer timer;
    private TextView button;
    private EditText userinput;
    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        button = findViewById(R.id.main_enter_btn);
        userinput = findViewById(R.id.main_location_et);

        //버튼 클릭하면
        button.setOnClickListener(view -> {
            destination = userinput.getText().toString();
            // Wi-Fi 스캔 시작
            startTask();
        });
    }

    public interface ApiService {
        @POST("find/{destination} ") // API 엔드포인트 설정
        Call<ResponseBody> sendLocationData(@Path(value = "destination") String destination, @Body JsonObject data);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
//                if(ssid.contains("GC_free_WiFi")){
                data.addProperty(bssid, rssi);
//                }
                Log.d(TAG, "SSID: " + ssid + ", BSSID: " + bssid + ", rssi: " + rssi);
            }
            sendLocationDataToServer(data);
        }
    }

    private void sendLocationDataToServer(JsonObject data) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(loggingInterceptor);

        // Retrofit 인스턴스 생성
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://finger.yanychoi.site/") // 서버 URL 설정
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 추가
                .build();

        JsonObject requestBody = new JsonObject();
        requestBody.add("signals", data);

        // 서비스 인터페이스 생성
        ApiService apiService = retrofit.create(ApiService.class);

        //POST 요청 보내기
        Call<ResponseBody> call = apiService.sendLocationData(destination, requestBody);

        //전송
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 요청이 성공적으로 전송된 경우
                } else {
                    // 요청이 실패한 경우
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
            }
        });

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("result", call.request().toString());
        startActivity(intent);
    }
}