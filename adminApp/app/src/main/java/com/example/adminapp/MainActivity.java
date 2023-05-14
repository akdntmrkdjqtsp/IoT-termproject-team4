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
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.ResponseBody;
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
    private Button button;
    private EditText userinput;
    private String location;
    private TextView scanresult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        button = findViewById(R.id.submit);
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
    }
    public interface ApiService {
        @POST("api/endpoint/{location}") // API 엔드포인트 설정
        Call<ResponseBody> sendLocationData(@Path(value = "location") String location, @Body JsonObject data);
    }

    private void scanWiFiNetworks() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
        }
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();
            JsonObject data = new JsonObject(); // 새로운 JsonObject 생성
            String ssid = null;
            String bssid = null;
            int rssi = 0;
            for (ScanResult scanResult : scanResults) {
                ssid = scanResult.SSID;
                bssid = scanResult.BSSID;
                rssi = scanResult.level;
                data.addProperty(bssid, rssi);
            }
            sendLocationDataToServer(data);
        }
    }

    private void sendLocationDataToServer(JsonObject data) {
                // Retrofit 인스턴스 생성
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://your-server-url.com/") // 서버 URL 설정
                .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 추가
                .build();

        // 서비스 인터페이스 생성
        ApiService apiService = retrofit.create(ApiService.class);

        //POST 요청 보내기
        Call<ResponseBody> call = apiService.sendLocationData(location, data);
        scanresult.setText(call.request().toString());
//        call.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    // 요청이 성공적으로 전송된 경우
//                    // 여기에서 필요한 추가 작업을 수행할 수 있습니다.
//                } else {
//                    // 요청이 실패한 경우
//                    // 여기에서 실패 처리를 수행할 수 있습니다.
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                // 네트워크 오류 등으로 요청이 실패한 경우
//                // 여기에서 실패 처리를 수행할 수 있습니다.
//            }
//        });
    }
}