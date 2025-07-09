package dev.vlab.tweetsms.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static dev.vlab.tweetsms.presentation.main.MainActivity.SMS_DELIVERED_ACTION;
import static dev.vlab.tweetsms.presentation.main.MainActivity.SMS_SENT_ACTION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.helper.SharedPrefManager;
import dev.vlab.tweetsms.helper.PusherOdk;
import dev.vlab.tweetsms.helper.UrlContainer;
import dev.vlab.tweetsms.presentation.main.MainActivity;

import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.PusherEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SmsWorkManager extends Worker {
    private static final String TAG = "SmsWorkManager";
    private static final String WORK_RESULT = "work_result";
    private final Context context;

    private static final int MAX_RETRIES_WORKER = 5; // Maximum number of retries
    private static final long RETRY_DELAY_MS_WORKER = 10000; // Delay between retries in milliseconds


    public SmsWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        Log.e(TAG, "IS CONNECTED " + manager.getStatus());

        if (manager.getStatus().equalsIgnoreCase("true")) {

            boolean isRunning = true;
            while (isRunning) {
                try {
                    implementPusher();
                    Thread.sleep(RETRY_DELAY_MS_WORKER);
                    if (manager.getStatus().equalsIgnoreCase("false")) {
                        Log.e(TAG, "LOOP BREAKED");
                        isRunning = false; // Exit the loop
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    Log.e(TAG, "Thread was interrupted", e);
                    return Result.failure(); // Return failure on interruption
                }
            }

        }

        Data outputData = new Data.Builder().putString(WORK_RESULT, "Jobs Finished").build();
        return Result.success(outputData);
    }


    private void implementPusher() {
        try {

            initPusher();
            setForegroundAsync(createForegroundInfo());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Pusher and notification: ", e);
        }
    }


    @SuppressLint("InlinedApi")
    @NonNull
    private ForegroundInfo createForegroundInfo() {
        Log.e(TAG, "NOTIFICATION ADDED TO PANEL");

        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "foreground_task_channel_id";
        String channelName = "SMS LAB Foreground Service Notification";

        // Create notification channel for Android O and above
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setVibrationPattern(new long[]{0});
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.enableLights(false);
        manager.createNotificationChannel(channel);

        // Create an Intent that will open the app when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class); // Replace MainActivity with the activity to open
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId).setContentTitle("Please don't close this notification").setContentText("Make sure your app is running in the background").setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
                .setContentIntent(pendingIntent) // Set the PendingIntent to be triggered on click
                .setVibrate(new long[]{0}).setSound(null).setAutoCancel(false);

        // Use a consistent notification ID
        int notificationId = 13256465; // Use the same ID to update the notification instead of creating a new one

        manager.notify(notificationId, builder.build());

        return new ForegroundInfo(notificationId, builder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    }


    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds delay between retries
    private int retryCount = 0;

    void initPusher() {
        try {
            SharedPrefManager manager = SharedPrefManager.getInstance(context);

            String channelsKey = manager.getPusherKey();
            String deviceID = manager.getDeviceId();
            String channelName = "private-message-send-" + deviceID;
            String cluster = manager.getPusherCluster();
//            String baseUrl = manager.getBaseUrl();
            String baseUrl = UrlContainer.getBaseUrl();
            String token = manager.getToken();

            if (!PusherOdk.isConnected) {

                // Initialize PusherOdk instance
                PusherOdk.resetInstance(baseUrl, channelsKey, cluster, token);
                PusherOdk pusherOdk = PusherOdk.getInstance(baseUrl, channelsKey, cluster, token);

                pusherOdk.connect();

                if (pusherOdk.getPusherApp().getPrivateChannel(channelName) == null) {
                    pusherOdk.getPusherApp().subscribePrivate(channelName, new PrivateChannelEventListener() {
                        @Override
                        public void onAuthenticationFailure(String message, Exception e) {
                            Log.e(TAG, "Authentication Failure: " + e.toString());
                            Log.e(TAG, "Authentication Failure: " + e.toString());
                            Log.e(TAG, "Details: " + baseUrl + " " + channelsKey + " " + cluster + " " + token);

                            if (retryCount < MAX_RETRIES) {
                                retryCount++;
                                Log.i(TAG, "Retrying... Attempt " + retryCount);
                                new Handler(Looper.getMainLooper()).postDelayed(() -> initPusher(), RETRY_DELAY_MS);
                            } else {
                                Log.e(TAG, "Max retries reached. Could not authenticate.");
                                // Notify the user or handle failure
                            }
                        }

                        @Override
                        public void onSubscriptionSucceeded(String channelName) {
                            Log.i(TAG, "Subscription Succeeded for channel: " + channelName);
                        }

                        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
                        @Override
                        public void onEvent(PusherEvent event) {
                            Log.d(TAG, "Event received: " + event.toString());
                            processEvent(event.toString());
                        }
                    }, "message-send");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Pusher initialization exception: " + e);
        }
    }


    private final Set<Integer> processedMessageIds = new HashSet<>();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    void processEvent(String event) {
        try {
            JSONObject object = new JSONObject(event);
            Log.i("EVENT-:-",object.toString());
            JSONObject data = new JSONObject(object.getString("data")).getJSONObject("data");
            String mDeviceId = SharedPrefManager.getInstance(context).getDeviceId();
            JSONObject originalData = data.getJSONObject("original_data");
            String deviceId = data.getString("device_id");

            if (deviceId.equals(mDeviceId)) {
                JSONArray messageList = originalData.getJSONArray("message");
                Log.i("ORGINALDATA-:-",originalData.toString());
                for (int i = 0; i < messageList.length(); i++) {
                    JSONObject msg = messageList.getJSONObject(i);
                    int messageId = msg.getInt("id");

                    if (processedMessageIds.add(messageId)) {  // Only add if it's not already processed
                        processMessage(msg);
                    } else {
                        Log.e(TAG, "Message ID " + messageId + " already processed, skipping.");
                    }
                }
            }

        } catch (Exception e) {
            Log.e("processEvent", "exception in on event method : " + e);
        }
    }

    private void processMessage(JSONObject msg) {
        try {
            String number = msg.getString("mobile_number");
            String message = msg.getString("message");
            String formattedMessage = msg.getString("formatted_message");
            String simSlot = msg.getString("device_slot_number");
            int messageId = msg.getInt("id");

            logCurrentTime();

            long delay = (long) Math.max(1, formattedMessage.length() / 153) * 2 * 1000;
            Thread.sleep(delay);

            if (formattedMessage.length() < 153) {
                Log.e(TAG, "send sms " + formattedMessage);
                sendSMS(number, formattedMessage, messageId, simSlot);
            } else {
                Log.e(TAG, "multipart message: " + formattedMessage);
             sendMultiPartSMS(number, formattedMessage, messageId, simSlot);
            }
        } catch (Exception e) {
            Log.e(TAG, "exception: " + e);
        }
    }


    private void logCurrentTime() {
        String dateToStr = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(new Date());
        Log.e(TAG, "time delay check: " + dateToStr);
    }


    public void sendSMS(String phoneNumber, String message, long messageId, String simSlot) {
        sendSMSInternal(phoneNumber, message, messageId, simSlot, false);
    }


    public void sendMultiPartSMS(String phoneNumber, String message, long messageId, String simSlot) {
        sendSMSInternal(phoneNumber, message, messageId, simSlot, true);
    }

    private void sendSMSInternal(String phoneNumber, String message, long messageId, String simSlot, boolean isMultipart) {
        int deliveryRequestCode = (int) messageId + 5000;
        int pendingRequestCode = deliveryRequestCode + 1;

        PendingIntent sentPI = createPendingIntent(SMS_SENT_ACTION, messageId, pendingRequestCode);
        PendingIntent deliveredPI = createPendingIntent(SMS_DELIVERED_ACTION, messageId, deliveryRequestCode);

        registerReceivers();

        int smsToSendFrom = getSimCardId(simSlot);

        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom);

        if (isMultipart) {
            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = createIntentList(sentPI, parts.size());
            ArrayList<PendingIntent> deliveryIntents = createIntentList(deliveredPI, parts.size());
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
        } else {
            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
        }
    }

    private PendingIntent createPendingIntent(String action, long messageId, int requestCode) {
        Intent intent = new Intent(action);
        intent.putExtra("SMS_ID", messageId);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }


    BroadcastReceiver sendBroadcastReceiver = new SentReceiver();
    BroadcastReceiver deliveredBroadcastReceiver = new DeliveredReceiver();


    @SuppressLint("NewApi")
    private void registerReceivers() {

        context.registerReceiver(sendBroadcastReceiver, new IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_EXPORTED);
        context.registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_EXPORTED);
    }

    private int getSimCardId(String simSlot) {
        ArrayList<Integer> simCardList = new ArrayList<>();
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                simCardList.add(subscriptionInfo.getSubscriptionId());
            }
        }
        return simCardList.get(simSlot.equals("1") ? 0 : 1);
    }

    private ArrayList<PendingIntent> createIntentList(PendingIntent baseIntent, int size) {
        ArrayList<PendingIntent> intents = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            intents.add(baseIntent);
        }
        return intents;
    }


}
