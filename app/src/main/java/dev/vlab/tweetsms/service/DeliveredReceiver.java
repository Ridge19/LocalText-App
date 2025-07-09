package dev.vlab.tweetsms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;

import dev.vlab.tweetsms.helper.UrlContainer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

class DeliveredReceiver extends BroadcastReceiver {

    String TAG = "TAG";

    @Override
    public void onReceive(Context context, Intent intent) {

        changeStatus(String.valueOf(intent.getLongExtra("SMS_ID", 0)), "Delivered", context);

    }

    void changeStatus(String messageId, String status, Context context) {


        Log.e(TAG, "message delivery status-------------------- " + status + "  msgid " + messageId);
        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);


        String mainStatus = status.equalsIgnoreCase("sent") ? "4" : status.equalsIgnoreCase("delivered") ? "1" : "9";
        String token = manager.getToken();

        Call<String> call = apiResponse.updateMessageStatus(token, messageId, mainStatus, ""); //1=delivered  2=pending 3=schedule 4=sent 9=fail

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.e(TAG, "server response : " + response.body());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });


    }
}


