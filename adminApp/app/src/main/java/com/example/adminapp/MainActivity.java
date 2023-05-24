package com.example.adminapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
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

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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
    //private static final String TAG = "WiFiScanner";

    private WifiManager wifiManager;
    private Button button;
    private EditText userinput;
    private String location;
    private TextView scanresult;
    private Button train;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        button = findViewById(R.id.submit);
        train = findViewById(R.id.train);
        userinput = findViewById(R.id.userinput);
        scanresult = findViewById(R.id.scanresult);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                location = userinput.getText().toString();
                // Wi-Fi 스캔 시작
                scanWiFiNetworks();
            }
        });

        train.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTrainToServer();
            }
        });
    }
    public interface ApiService {
        @POST("store/{location}") // API 엔드포인트 설정
        Call<ResponseBody> sendLocationData(@Path(value = "location") String location, @Body JsonObject data);
    }

    public interface ApiTrain {
        @POST("train") // API 엔드포인트 설정
        Call<ResponseBody> sendTrainData();
    }

    private void scanWiFiNetworks() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
        }
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();//스캔 시작
            List<ScanResult> scanResults = wifiManager.getScanResults();//스캔 결과 리스트로 받음
            JsonObject data = new JsonObject(); // 새로운 JsonObject 생성
            String ssid;
            String bssid;
            int rssi;
            for (ScanResult scanResult : scanResults) {
                ssid = scanResult.SSID;
                bssid = scanResult.BSSID;
                rssi = (scanResult.level + 100)*2;
                if(ssid.contains("GC_free_WiFi") || ssid.contains("eduroam")){
                    data.addProperty(bssid, rssi);
                }

            }
            sendLocationDataToServer(data);
        }
    }

    private void sendLocationDataToServer(JsonObject data) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();//이하4줄 &.client()지워야함,그래들도
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
        Call<ResponseBody> call = apiService.sendLocationData(location, requestBody);
        scanresult.setText(call.request().toString());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 요청이 성공적으로 전송된 경우
                    // 여기에서 필요한 추가 작업을 수행할 수 있습니다.
                    System.out.println(response.headers()+ " 성공");
                    try {
                        System.out.println(response.body().string()+ " 성공");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 요청이 실패한 경우
                    // 여기에서 실패 처리를 수행할 수 있습니다.
                    System.out.println(response.code() + " 실패");
                    System.out.println(response + " 실패");
                    try {
                        System.out.println(response.body().string()+ " 실패");
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
                // 여기에서 실패 처리를 수행할 수 있습니다.
            }
        });
    }

    private void sendTrainToServer() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();//이하4줄 &.client()지워야함,그래들도
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(loggingInterceptor);
        // Retrofit 인스턴스 생성
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://finger.yanychoi.site/") // 서버 URL 설정
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 추가
                .build();

        ApiTrain apiTrain = retrofit.create(ApiTrain.class);

        //POST 요청 보내기
        Call<ResponseBody> call = apiTrain.sendTrainData();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 요청이 성공적으로 전송된 경우
                    // 여기에서 필요한 추가 작업을 수행할 수 있습니다.
                    System.out.println(response.headers()+ " 성공");
                    try {
                        System.out.println(response.body().string()+ " 성공");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 요청이 실패한 경우
                    // 여기에서 실패 처리를 수행할 수 있습니다.
                    System.out.println(response.code() + " 실패");
                    System.out.println(response + " 실패");
                    try {
                        System.out.println(response.body().string()+ " 실패");
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 네트워크 오류 등으로 요청이 실패한 경우
                // 여기에서 실패 처리를 수행할 수 있습니다.
            }
        });
    }
}