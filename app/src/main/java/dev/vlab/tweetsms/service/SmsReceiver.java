package dev.vlab.tweetsms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "SMS received via SMS_DELIVER");
            // Handle incoming SMS
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            for (SmsMessage message : messages) {
                String body = message.getMessageBody();
                String sender = message.getOriginatingAddress();
                Log.d(TAG, "SMS from " + sender + ": " + body);
            }
        }
    }
}
