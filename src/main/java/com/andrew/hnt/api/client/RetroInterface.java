package com.andrew.hnt.api.client;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.Map;

public interface RetroInterface {

    @POST("main/sendAlarm")
    Call<Map<String, Object>> sendAlarm(
            @Body Map<String, Object> param
    );
}
