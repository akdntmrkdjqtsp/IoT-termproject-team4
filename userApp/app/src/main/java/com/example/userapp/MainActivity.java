package com.example.userapp;


import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

public class MainActivity extends AppCompatActivity {
    AppCompatSpinner spinner;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView startButton = findViewById(R.id.main_enter_btn);
        spinner = findViewById(R.id.main_location_sp);

        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.item_spinner, getResources().getStringArray(R.array.classroom));
        spinner.setAdapter(areaAdapter);

        // 안내 시작 버튼을 클릭하면
        startButton.setOnClickListener(view -> {
            String destination = spinner.getSelectedItem().toString();

            if(!destination.isEmpty()) {
                // 네비게이션 화면으로 넘어감
                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.putExtra("destination", destination);
                intent.putExtra("destinationAPI", getClassroom());

                startActivity(intent);
            }
        });
    }

    private String getClassroom() {
        String destination = spinner.getSelectedItem().toString();

        /*
            <item>406호</item>
            <item>407호</item>
            <item>413호</item>
            <item>414호</item>
            <item>4층 아르테크네</item>
            <item>4층 휴게 공간</item>
            <item>4층 엘리베이터(동)</item>
            <item>4층 엘리베이터(중)</item>
            <item>4층 계단</item>
            <item>506호</item>
            <item>508호</item>
            <item>510호</item>
            <item>513호</item>
            <item>큐브 입구 1</item>
            <item>큐브 입구 2</item>
            <item>5층 엘리베이터 1(동)</item>
            <item>5층 엘리베이터 2(중)</item>
            <item>5층 엘리베이터 3(서)</item>
            서쪽이 3 중앙이 4 동이 1
         */

        switch (destination) {
            case "406호" :
                destination = "406";
                break;
            case "407호" :
                destination = "407-indoor";
                break;
            case "413호" :
                destination = "413-indoor";
                break;
            case "414호" :
                destination = "414-indoor";
                break;
            case "4층 아르테크네" :
                destination = "artechne-4";
                break;
            case "4층 휴게 공간" :
                destination = "rest-area";
                break;
            case "4층 엘리베이터(동)" :
                destination = "elevator-1-4F";
                break;
            case "4층 엘리베이터(중)" :
                destination = "elevator-4-4F";
                break;
            case "4층 계단" :
                destination = "stair-5-4F";
                break;

            case "506호" :
                destination = "506";
                break;
            case "508호" :
                destination = "508-indoor";
                break;
            case "510호" :
                destination = "510";
                break;
            case "513호" :
                destination = "513";
                break;
            case "큐브 입구 1" :
                destination = "cube";
                break;
            case "큐브 입구 2" :
                destination = "cube";
                break;
            case "5층 엘리베이터(동)" :
                destination = "elevator-1-5F";
                break;
            case "5층 엘리베이터(중)" :
                destination = "elevator-4-5F";
                break;
            case "5층 엘리베이터(서)" :
                destination = "elevator-3-5F";
                break;
        }

        return destination;
    }
}