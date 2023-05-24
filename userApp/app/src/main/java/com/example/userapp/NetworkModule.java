package com.example.userapp;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkModule {
    private static Retrofit retrofit = null;

    public static Retrofit getRestrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(loggingInterceptor);

        // Retrofit 인스턴스 생성
        retrofit = new Retrofit.Builder()
                .baseUrl("https://finger.yanychoi.site/") // 서버 URL 설정
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 추가
                .build();

        return retrofit;
    }
}
