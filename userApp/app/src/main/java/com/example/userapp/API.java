package com.example.userapp;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface API {
    @POST("find/{destination} ") // API 엔드포인트 설정
    Call<ResponseBody> sendLocationData(@Path(value = "destination") String destination, @Body JsonObject data);
}
