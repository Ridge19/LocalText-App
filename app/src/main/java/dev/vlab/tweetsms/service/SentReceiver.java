package dev.vlab.tweetsms.service;

import static dev.vlab.tweetsms.presentation.main.MainActivity.SMS_SENT_ACTION;

import android.app.Activity;
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

class SentReceiver extends BroadcastReceiver {

    String TAG = "TAG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String message;
        String errorDetail = "";

        long smsId = intent.getLongExtra("SMS_ID", 0);
        Log.e(TAG, "SMS Result Code for ID " + smsId + ": " + getResultCode());

        String action = intent.getAction();
        if (SMS_SENT_ACTION.equals(action)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = "Delivered";
                    Log.i(TAG, "SMS " + smsId + " sent successfully");
                    changeStatus(String.valueOf(smsId), message, context, "");
                    break;

                case android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = "Failed";
                    errorDetail = "Generic failure";
                    Log.e(TAG, "SMS " + smsId + " failed: Generic failure");
                    changeStatus(String.valueOf(smsId), message, context, getResultCode() + " - " + errorDetail);
                    break;

                case android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "Failed";
                    errorDetail = "No service";
                    Log.e(TAG, "SMS " + smsId + " failed: No service");
                    changeStatus(String.valueOf(smsId), message, context, getResultCode() + " - " + errorDetail);
                    break;

                case android.telephony.SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "Failed";
                    errorDetail = "Null PDU";
                    Log.e(TAG, "SMS " + smsId + " failed: Null PDU");
                    changeStatus(String.valueOf(smsId), message, context, getResultCode() + " - " + errorDetail);
                    break;

                case android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "Failed";
                    errorDetail = "Radio off";
                    Log.e(TAG, "SMS " + smsId + " failed: Radio off");
                    changeStatus(String.valueOf(smsId), message, context, getResultCode() + " - " + errorDetail);
                    break;

                default:
                    message = "Failed";
                    errorDetail = "Unknown error";
                    Log.e(TAG, "SMS " + smsId + " failed with unknown error code: " + getResultCode());
                    changeStatus(String.valueOf(smsId), message, context, getResultCode() + " - " + errorDetail);


            }
        }
    }

    void changeStatus(String messageId, String status, Context context, String errorCode) {

        Log.e(TAG, "message  status-------------------- " + status + "  msgid " + messageId);
        Log.e(TAG, "changeStatus: " + status + " message id: " + messageId);
        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);


        String mainStatus = status.equalsIgnoreCase("sent") ? "4" : status.equalsIgnoreCase("delivered") ? "1" : "9";
        String token = manager.getToken();

        Log.e(TAG, "message update ------ : " + mainStatus + " message id : " + messageId);


        Call<String> call = apiResponse.updateMessageStatus(token, messageId, mainStatus, errorCode); //1=delivered  2=pending 3=schedule 4=sent 9=fail

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


