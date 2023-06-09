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

        switch (destination) {
            case "405호" :
                destination = "405";
                break;

            case "412호" :
                destination = "412";
                break;

            case "418호" :
                destination = "418";
                break;

            case "433호" :
                destination = "433";
                break;

            case "4층 아르테크네" :
                destination = "artechne-4";
                break;

            case "505호" :
                destination = "505";
                break;

            case "512호" :
                destination = "512";
                break;

            case "520호" :
                destination = "520";
                break;

            case "531호" :
                destination = "531";
                break;

            case "5층 아르테크네" :
                destination = "artechne-5";
                break;
        }

        return destination;
    }
}