package dev.vlab.tweetsms.service;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.vlab.tweetsms.helper.UrlContainer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MessageReceiver extends BroadcastReceiver {

    String TAG = "TAG";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {

            for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {

                Log.e(TAG, "onReceive: " + message.toString());
                String mes = message.getMessageBody();
                String sender = message.getDisplayOriginatingAddress().trim();


                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    detectSim(bundle, context);
                }
                String simName = null;
                if (bundle != null) {
                    simName = getSimName(bundle, context);
                }

                String fullMessage = getMessageBody(bundle);
                if (!fullMessage.isEmpty()) {
                    mes = fullMessage;
                }


                SharedPrefManager manager = SharedPrefManager.getInstance(context);

                String token = manager.getToken();
                String deviceId = manager.getDeviceId();

                if (manager.getStatus().equalsIgnoreCase("true")) {
                    sendMessageToServer(token, deviceId, sender, mes, simName);
                }
                break;
            }
        }


    }


    private void sendMessageToServer(String token, String deviceId, String sender, String mes, String simName) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        /* headers.put("session-id",session);*/
        headers.put("Authorization", token);


        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);
        int simSlot = simSlotId + 1;  //index will start from 1
        Call<String> call = apiResponse.uploadMessage(token, deviceId, sender, mes, simName, simSlot + "");

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {

            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {

            }
        });
    }

    private String getMessageBody(Bundle bundle) {
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) {
                return "";
            }

            StringBuilder content = new StringBuilder();

            for (Object o : pdus) {

                SmsMessage message = SmsMessage.createFromPdu((byte[]) o, bundle.getString("format"));
                if (message != null) {
                    content.append(message.getMessageBody());
                }
            }

            return content.toString();
        }

        return "";

    }


    private String getSimName(Bundle bundle, Context context) {

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return "Undefined";
        }

        // Determine the format of the SMS message
        String format = bundle.getString("format");

        // Use SmsMessage.createFromPdu with format
        SmsMessage shortMessage = SmsMessage.createFromPdu((byte[]) pdus[0], format);

        int t = shortMessage.getIndexOnIcc();
        Log.e(TAG, "getSimName: " + t);

        String simName;

        int sub = bundle.getInt("subscription", -1);
        SubscriptionManager manager = SubscriptionManager.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Undefined";
        }

        SubscriptionInfo subInfo = manager.getActiveSubscriptionInfo(sub);
        if (subInfo != null) {
            simName = subInfo.getCarrierName().toString();
        } else {
            simName = "Undefined";
        }

        return simName;
    }

    int simSlotId;

    private void detectSim(Bundle bundle, Context context) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int slot = -1;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            Log.e(TAG, "all key: " + key);
            if (bundle.containsKey(key)) {
                Object value = bundle.get(key);
                if (value instanceof Integer) {
                    slot = (Integer) value;
                } else if (value instanceof String) {
                    String stringValue = (String) value;
                    if (stringValue.equals("0") || stringValue.equals("1") || stringValue.equals("2")) {
                        slot = Integer.parseInt(stringValue);
                    }
                }
            }
        }

        simSlotId = slot;
    }


}
