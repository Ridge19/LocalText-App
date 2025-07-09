package dev.vlab.tweetsms.apiclient;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitInstance {


    public static Retrofit getRetrofitInstance(String baseUrl) {
        Log.e("RETRO",baseUrl);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                //.baseUrl(baseUrl + "api/v1/")
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();


    }


}
