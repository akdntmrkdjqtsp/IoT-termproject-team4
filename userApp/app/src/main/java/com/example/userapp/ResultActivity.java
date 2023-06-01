package com.example.userapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "WiFiScanner";

    private WifiManager wifiManager;
    private Timer timer;

    private SensorManager sensorManager;
    private SensorListener sensorListener;
    private ScheduledExecutorService scheduledExecutor;
    private TextView start;
    private TextView current;
    private TextView end;

    private ImageView nextArrow;
    private TextView nextRemain;
    private ImageView arrow;
    private TextView remain;

    private String destination = "";
    private double newDirection = 0;
    private double nextDirection = 0;

    private boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // 와이파이 측정
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        // 목적지 입력
        start = findViewById(R.id.result_start_tv);
        current = findViewById(R.id.result_now_tv);
        end = findViewById(R.id.result_destination_tv);

        end.setText("도착지: " + getIntent().getStringExtra("destination"));
        destination = getIntent().getStringExtra("destination");


        // 화면 기본 세팅
        nextArrow = findViewById(R.id.result_next_arrow_iv);
        nextRemain = findViewById(R.id.result_next_remain_tv);
        nextArrow.setImageResource(R.drawable.ic_up_s);
        nextRemain.setText("남은 거리 : \n0");

        arrow = findViewById(R.id.result_arrow_iv);
        remain = findViewById(R.id.result_remain_tv);
        arrow.setImageResource(R.drawable.ic_up);
        remain.setText("남은 거리 : 0");

        // 방향 측정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorListener = new SensorListener();

        startSensor();


        // 안내 종료
        TextView endBtn = findViewById(R.id.result_end_btn);
        endBtn.setOnClickListener(view-> {
            stopTask();
            stopSensor();
            finish();
        });

        startTask();
    }

    public void startSensor() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopSensor() {
        sensorManager.unregisterListener(sensorListener);
    }

    private void startTask() {
        // 1초마다 작업 실행
        long intervalMillis = 1000;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // 작업 실행
                scanWiFiNetworks();
            }
        }, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void stopTask() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
            }
            scheduledExecutor = null;
        }
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

                if(ssid.contains("GC_free_WiFi") || ssid.contains("eduroam")){
                    data.addProperty(bssid, rssi);
                    String finalBssid = bssid;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView debug = findViewById(R.id.debug);
                            debug.setText(finalBssid);
                        }
                    });

                    Log.d(TAG, "SSID: " + ssid + ", BSSID: " + bssid + ", rssi: " + rssi);

                    sendLocationDataToServer(data);
                }

                // data.addProperty(bssid, rssi);
                // 414 : SSID: AndroidWifi, BSSID: 00:13:10:85:fe:01, rssi: 100
            }
        }
    }

    class SensorListener implements SensorEventListener {
        float[] accValue = new float[3];
        float[] magValue = new float[3];

        boolean isGetAcc = false;
        boolean isGetMag = false;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int type = sensorEvent.sensor.getType();

            switch (type) {
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(sensorEvent.values, 0, accValue, 0, sensorEvent.values.length);
                    isGetAcc = true;
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(sensorEvent.values, 0, magValue, 0, sensorEvent.values.length);
                    isGetMag = true;
                    break;
            }

            if(isGetAcc && isGetMag) {
                float[] R = new float[9];
                float[] I = new float[9];

                // 행렬 계산
                SensorManager.getRotationMatrix(R, I, accValue, magValue);

                // 계산한 결과를 방위값으로 환산
                float[] values = new float[3];
                SensorManager.getOrientation(R, values);

                // 방위값을 각도 단위로 변경
                float azimuth = (float) Math.toDegrees(values[0]);  // 방위값
                float pitch = (float) Math.toDegrees(values[1]);  // 좌우 기울기
                float roll = (float) Math.toDegrees(values[2]);  // 앞뒤 기울기

                if(String.valueOf(azimuth).equals("-0.0")) azimuth = 0;
                else if(String.valueOf(azimuth).equals("0.0")) azimuth = 180;

                // Log.d("방위각 :", String.valueOf(azimuth));

                azimuth = azimuth < 0 ? (azimuth + 360) : azimuth;
                setArrowImg(360 - (azimuth - newDirection));

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

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
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject object = new JSONObject(response.body().string());

                        Log.d("FIND-SUC", object.toString());

                        int startPt = object.getInt("start");
                        int endPt =  object.getInt("end");

                        Log.d("start, end", startPt + " " + endPt);

                        if(first) {
                            start.setText("출발지: " + startPt);
                            first = false;
                        }

                        current.setText("현 위치: " + startPt);
                        end.setText("목적지: " + endPt);

                        JSONArray jsonArray = object.getJSONArray("path");

                        if(jsonArray.length() == 0) {
                            remain.setText("목적지에 도착했습니다.");
                            stopSensor();
                            stopTask();
                        }

                        for(int i = 0; i < jsonArray.length(); i++){
                            JSONObject cur = (JSONObject) jsonArray.get(i);//인덱스 번호로 접근해서 가져온다.

                            /*
                            {
                              "start": "409",
                              "end": "426",
                              "path": [
                                {
                                  "cardinal_direction": "20",
                                  "distance": "20.0"
                                },
                                {
                                  "cardinal_direction": "230",
                                  "distance": "83.0"
                                }
                              ]
                            }
                             */

                            int direction = cur.getInt("cardinal_direction");
                            int distance =  cur.getInt("distance");

                            System.out.println("----- "+i+"번째 인덱스 값 -----");
                            System.out.println("방향 : " + direction);
                            System.out.println("거리 : " + distance);

                            if(i == 0) {
                                remain.setText("남은 거리 : " + distance);
                                newDirection = direction;
                                setArrowImg(direction);
                            } else if(i == 1) {
                                nextRemain.setText("남은 거리 : \n" + distance);
                                nextDirection = direction;
                                setSmallArrowImg(direction);
                            }
                        }

                    } catch (JSONException | IOException e) {
                        System.out.println("API 연결에 실패했습니다.");
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

    public void setArrowImg(double direction) {
        if((340 <= direction && direction < 360) || (0 <= direction && direction <= 20)) arrow.setImageResource(R.drawable.ic_up);
        else if (20 < direction && direction < 70) arrow.setImageResource(R.drawable.ic_right_up);
        else if (70 <= direction && direction <= 110) arrow.setImageResource(R.drawable.ic_right);
        else if (110 < direction && direction < 160) arrow.setImageResource(R.drawable.ic_right_down);
        else if (160 <= direction && direction <= 200) arrow.setImageResource(R.drawable.ic_down);
        else if (200 < direction && direction < 250) arrow.setImageResource(R.drawable.ic_left_down);
        else if (250 <= direction && direction <= 290) arrow.setImageResource(R.drawable.ic_left);
        else if (290 < direction && direction < 340) arrow.setImageResource(R.drawable.ic_left_up);
    }

    public void setSmallArrowImg(double direction) {
        if((340 <= direction && direction < 360) || (0 <= direction && direction <= 20)) nextArrow.setImageResource(R.drawable.ic_up_s);
        else if (20 < direction && direction < 70) nextArrow.setImageResource(R.drawable.ic_right_up_s);
        else if (70 <= direction && direction <= 110) nextArrow.setImageResource(R.drawable.ic_right_s);
        else if (110 < direction && direction < 160) nextArrow.setImageResource(R.drawable.ic_right_down_s);
        else if (160 <= direction && direction <= 200) nextArrow.setImageResource(R.drawable.ic_down_s);
        else if (200 < direction && direction < 250) nextArrow.setImageResource(R.drawable.ic_left_down_s);
        else if (250 <= direction && direction <= 290) nextArrow.setImageResource(R.drawable.ic_left_s);
        else if (290 < direction && direction < 340) nextArrow.setImageResource(R.drawable.ic_left_up_s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티가 종료될 때 작업 중지
        stopTask();
        stopSensor();
    }
}