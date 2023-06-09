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
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
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

    private boolean active = true;

    private TextView start;
    private TextView current;
    private TextView end;

    private ImageView nextArrow;
    private TextView nextRemain;
    private ImageView arrow;
    private TextView remain;

    private String destinationAPI = "";

    private double newDirection = 0;
    private double nextDirection = 0;

    private boolean first = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        active = true;

        // 와이파이 측정
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 목적지 입력
        start = findViewById(R.id.result_start_real_tv);
        current = findViewById(R.id.result_now_real_tv);
        end = findViewById(R.id.result_destination_real_tv);

        String destination = getIntent().getStringExtra("destination");
        destinationAPI = getIntent().getStringExtra("destinationAPI");

        end.setText(destination);

        // 화면 기본 세팅
        nextArrow = findViewById(R.id.result_next_arrow_iv);
        nextRemain = findViewById(R.id.result_next_remain_tv);
        nextArrow.setImageResource(R.drawable.ic_up_s);
        nextRemain.setText("남은 거리 :\n0m");

        arrow = findViewById(R.id.result_arrow_iv);
        remain = findViewById(R.id.result_remain_tv);
        arrow.setImageResource(R.drawable.ic_up);
        remain.setText("남은 거리 : 0m");

        // 방향 측정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorListener = new SensorListener();

        // 안내 종료
        TextView endBtn = findViewById(R.id.result_end_btn);
        endBtn.setOnClickListener(view-> {
            stopSensor();
            active = false;

            finish();
        });

        startSensor();
        scanWiFiNetworks();
    }

    public void startSensor() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopSensor() {
        sensorManager.unregisterListener(sensorListener);
    }

    private Runnable scanWiFiNetworks() {
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
                    Log.d(TAG, "SSID: " + ssid + ", BSSID: " + bssid + ", rssi: " + rssi);
                }

                // data.addProperty(bssid, rssi);
                // 414 : SSID: AndroidWifi, BSSID: 00:13:10:85:fe:01, rssi: 100
            }

            sendLocationDataToServer(data);
        }

        return null;
    }

    class SensorListener implements SensorEventListener {
        float[] accValue = new float[3];
        float[] magValue = new float[3];

        boolean isGetAcc = false;
        boolean isGetMag = false;
        boolean isGetGyro = false;

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

                case Sensor.TYPE_GYROSCOPE:
                    isGetGyro = true;
                    break;
            }

            if(isGetAcc && isGetMag && isGetGyro) {
                float[] R = new float[9];
                float[] I = new float[9];

                // 행렬 계산
                SensorManager.getRotationMatrix(R, I, accValue, magValue);

                // 계산한 결과를 방위값으로 환산
                float[] values = new float[3];
                SensorManager.getOrientation(R, values);

                // 방위값을 각도 단위로 변경
                float azimuth = (float) Math.toDegrees(values[0]);  //
                float pitch = (float) Math.toDegrees(values[1]);  // 좌우 기울기
                float roll = (float) Math.toDegrees(values[2]);  // 앞뒤 기울기

                if(String.valueOf(pitch).equals("-0.0")) pitch = 0;
                else if(String.valueOf(pitch).equals("0.0")) pitch = 180;

                // Log.d("방위각 :", String.valueOf(pitch));

                pitch = pitch < 0 ? (pitch + 360) : pitch;
                setArrowImg(360 - (pitch - newDirection));

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private void sendLocationDataToServer(JsonObject data) {
        JsonObject requestBody = new JsonObject();
        requestBody.add("signals", data);

        API apiService = NetworkModule.getRestrofit().create(API.class);
        // 서비스 인터페이스 생성
        apiService.sendLocationData(destinationAPI, requestBody).enqueue(new Callback<ResponseBody>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                // 요청이 성공적으로 전송된 경우

                if(response.body() != null) {
                    if(response.isSuccessful()) {
                        try {
                            JSONObject object = new JSONObject(response.body().string());

                            Log.d("FIND-SUC", object.toString());

                            String startPt = object.getString("start");
                            String endPt =  object.getString("end");

                            Log.d("start, end", startPt + " " + endPt);

                            startPt = getClassroom(startPt);

                            if(first) {
                                start.setText(startPt);
                                first = false;
                            }

                            current.setText(startPt);

                            if(start.equals(end)) {
                                remain.setText("목적지에 도착했습니다.");
                                stopSensor();
                            } else {
                                JSONArray jsonArray = object.getJSONArray("path");
                                if(jsonArray.length() == 0) {
                                    arrow.setVisibility(View.GONE);
                                    remain.setVisibility(View.GONE);

                                    TextView warning = findViewById(R.id.result_warning_tv);
                                    warning.setVisibility(View.VISIBLE);
                                } else {
                                    for(int i = 0; i < jsonArray.length(); i++) {
                                        arrow.setVisibility(View.VISIBLE);
                                        remain.setVisibility(View.VISIBLE);

                                        TextView warning = findViewById(R.id.result_warning_tv);
                                        warning.setVisibility(View.GONE);

                                        //인덱스 번호로 접근해서 가져온다.
                                        JSONObject cur = (JSONObject) jsonArray.get(i);

                                        int direction = cur.getInt("cardinal_direction");
                                        int distance = cur.getInt("distance");

                                        System.out.println("----- " + i + "번째 인덱스 값 -----");
                                        System.out.println("방향 : " + direction);
                                        System.out.println("거리 : " + distance);

                                        if (i == 0) {
                                            remain.setText("남은 거리 : " + distance + "m");
                                            newDirection = direction;
                                            setArrowImg(direction);
                                        } else if (i == 1) {
                                            nextRemain.setText("남은 거리 : \n" + distance + "m");
                                            nextDirection = direction;
                                            setSmallArrowImg(direction);
                                        }
                                    }
                                }
                            }

                            if(active) {
                                // 1초 후에 다시 API를 호출
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(scanWiFiNetworks(), 1500);
                            }

                        } catch (JSONException | IOException e) {
                            System.out.println("API 연결에 실패했습니다.");

                            if(active) {
                                // 1초 후에 다시 API를 호출
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(scanWiFiNetworks(), 1500);
                            }
                        }
                    }
                } else {
                    if(response.errorBody() != null) {
                        try {
                            Log.d("FIND-FAIL", response.errorBody().string());
                            String nowPt = response.errorBody().string();

                            Log.d("FIND-FAIL", nowPt);

                            if(first) {
                                start.setText(nowPt);
                                first = false;
                            }

                            current.setText(nowPt);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
                if(active) {
                    // 1초 후에 다시 API를 호출
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(scanWiFiNetworks(), 1500);
                }
            }
        });
    }

    private String getClassroom(String startPt) {
        if(startPt.length() == 3) startPt += "호";  // 일반 강의실의 경우 ~호 형태로 변환
        else if(startPt.contains("indoor")) startPt = startPt.replace("-indoor", "호");
        else if(startPt.contains("hall")) startPt = startPt.replace("hall", "-A");
        else if(startPt.equals("artechne-4")) startPt = "4층 아르테크네";
        else if(startPt.equals("artechne-5")) startPt = "5층 아르테크네";
        else if(startPt.equals("cube")) startPt = "큐브";
        else if(startPt.contains("bathroom")) startPt = "화장실";
        else if(startPt.contains("elevator") && startPt.contains("4F")) startPt = "4층 엘리베이터";
        else if(startPt.contains("elevator") && startPt.contains("5F")) startPt = "5층 엘리베이터";
        else if(startPt.contains("stair") && startPt.contains("4F")) startPt = "4층 계단";
        else if(startPt.contains("stair") && startPt.contains("5F")) startPt = "5층 계단";

        return startPt;
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
        stopSensor();
    }
}