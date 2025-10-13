package com.andrew.hnt.api.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
// import okhttp3.internal.JavaNetCookieJar; // Java 8 호환성 문제로 주석 처리
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroClient {
    private static final String BASE_URL = "http://localhost:8888/";

    public static RetroInterface getApiService() {
        return getInstance().create(RetroInterface.class);
    }

    private static Retrofit getInstance(){
        Gson gson = new GsonBuilder().setLenient().create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 쿠키 관리자 설정 (Java 8 호환성 문제로 주석 처리)
        // CookieManager cookieManager = new CookieManager();
        // CookieHandler.setDefault(cookieManager);
        OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(interceptor)
                // .cookieJar(new JavaNetCookieJar(cookieManager)) // Java 8 호환성 문제로 주석 처리
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}