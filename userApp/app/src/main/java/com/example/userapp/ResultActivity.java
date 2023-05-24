package com.example.userapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    ImageView nextArrow;
    TextView nextRemain;

    ImageView arrow;
    TextView remain;

    String destination = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 목적지 입력
        TextView result = findViewById(R.id.result_destination_tv);
        result.setText(getIntent().getStringExtra("destination"));
        destination = getIntent().getStringExtra("destination");

        nextArrow = findViewById(R.id.result_next_arrow_iv);
        nextRemain = findViewById(R.id.result_next_remain_tv);

        arrow = findViewById(R.id.result_arrow_iv);
        remain = findViewById(R.id.result_remain_tv);

        // 화면 기본 세팅
        nextArrow.setImageResource(R.drawable.ic_up_s);
        nextRemain.setText("남은 거리 : \n0");

        arrow.setImageResource(R.drawable.ic_up);
        remain.setText("남은 거리 : 0");


        // 안내 종료
        TextView endBtn = findViewById(R.id.result_end_btn);
        endBtn.setOnClickListener(view-> {
            stopTask();
            finish();
        });

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

//                if(ssid.contains("GC_free_WiFi") || ssid.contains("eduroam")){
//                    data.addProperty(bssid, rssi);
//                }

                data.addProperty(bssid, rssi);

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
        apiService.sendLocationData(destination, requestBody).enqueue(new Callback<ResponseBody>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                // 요청이 성공적으로 전송된 경우
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        try {
                            JSONArray jsonArray = new JSONArray(response.body().string());
                            Log.d("FIND-SUC", jsonArray.toString());

                            for(int i = 0; i < jsonArray.length(); i++){
                                JSONObject cur = (JSONObject) jsonArray.get(i);//인덱스 번호로 접근해서 가져온다.

                                double direction = cur.getDouble("cardinal_direction");
                                double distance =  cur.getDouble("distance");

                                System.out.println("----- "+i+"번째 인덱스 값 -----");
                                System.out.println("방향 : " + direction);
                                System.out.println("거리 : " + distance);

                                if(i == 0) {
                                    remain.setText("남은 거리 : " + distance);

                                    if((340 <= direction && direction < 360) || (0 <= direction && direction <= 20)) arrow.setImageResource(R.drawable.ic_up);
                                    else if (20 < direction && direction < 70) arrow.setImageResource(R.drawable.ic_right_up);
                                    else if (70 <= direction && direction <= 110) arrow.setImageResource(R.drawable.ic_right);
                                    else if (110 < direction && direction < 160) arrow.setImageResource(R.drawable.ic_right_down);
                                    else if (160 <= direction && direction <= 200) arrow.setImageResource(R.drawable.ic_down);
                                    else if (200 < direction && direction < 250) arrow.setImageResource(R.drawable.ic_left_down);
                                    else if (250 <= direction && direction <= 290) arrow.setImageResource(R.drawable.ic_left);
                                    else if (290 < direction && direction < 340) arrow.setImageResource(R.drawable.ic_left_up);

                                } else if(i == 1) {
                                    nextRemain.setText("남은 거리 : \n" + distance);

                                    if((340 <= direction && direction < 360) || (0 <= direction && direction <= 20)) nextArrow.setImageResource(R.drawable.ic_up_s);
                                    else if (20 < direction && direction < 70) nextArrow.setImageResource(R.drawable.ic_right_up_s);
                                    else if (70 <= direction && direction <= 110) nextArrow.setImageResource(R.drawable.ic_right_s);
                                    else if (110 < direction && direction < 160) nextArrow.setImageResource(R.drawable.ic_right_down_s);
                                    else if (160 <= direction && direction <= 200) nextArrow.setImageResource(R.drawable.ic_down_s);
                                    else if (200 < direction && direction < 250) nextArrow.setImageResource(R.drawable.ic_left_down_s);
                                    else if (250 <= direction && direction <= 290) nextArrow.setImageResource(R.drawable.ic_left_s);
                                    else if (290 < direction && direction < 340) nextArrow.setImageResource(R.drawable.ic_left_up_s);
                                }
                            }

                        } catch (JSONException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
                Log.d("FAIL", t.getMessage());
            }
        });
    }
}