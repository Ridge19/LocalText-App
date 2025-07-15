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

            // Add carrier-specific delays to avoid being blocked
            long baseDelay = Math.max(1, formattedMessage.length() / 153) * 2 * 1000;
            long carrierDelay = getCarrierSpecificDelay();
            long totalDelay = baseDelay + carrierDelay;

            Log.i(TAG, "Applying delay: " + totalDelay + "ms (base: " + baseDelay + "ms, carrier: " + carrierDelay + "ms)");
            Thread.sleep(totalDelay);

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

    /**
     * Get carrier-specific delay to avoid being blocked
     */
    private long getCarrierSpecificDelay() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                    SubscriptionInfo primarySim = subscriptionInfoList.get(0);
                    String carrierName = primarySim.getCarrierName().toString().toLowerCase();

                    Log.i(TAG, "Carrier: " + carrierName);

                    // Get MCC/MNC only on API 29+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        String mccMnc = primarySim.getMccString() + primarySim.getMncString();
                        Log.i(TAG, "MCC/MNC: " + mccMnc);
                    } else {
                        Log.i(TAG, "MCC/MNC: Not available (API < 29)");
                    }

                    // Carrier-specific delays (in milliseconds)
                    if (carrierName.contains("verizon")) {
                        return 3000; // 3 seconds for Verizon
                    } else if (carrierName.contains("at&t") || carrierName.contains("att")) {
                        return 2500; // 2.5 seconds for AT&T
                    } else if (carrierName.contains("t-mobile") || carrierName.contains("tmobile")) {
                        return 2000; // 2 seconds for T-Mobile
                    } else if (carrierName.contains("sprint")) {
                        return 2500; // 2.5 seconds for Sprint
                    } else if (carrierName.contains("vodafone")) {
                        return 3000; // 3 seconds for Vodafone
                    } else if (carrierName.contains("orange")) {
                        return 2500; // 2.5 seconds for Orange
                    } else if (carrierName.contains("airtel")) {
                        return 2000; // 2 seconds for Airtel
                    } else if (carrierName.contains("jio") || carrierName.contains("reliance")) {
                        return 1500; // 1.5 seconds for Jio
                    } else {
                        // Default conservative delay for unknown carriers
                        return 2000; // 2 seconds default
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting carrier info: " + e.getMessage());
        }

        return 2000; // Default 2 seconds if can't determine carrier
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
        // Check SMS permission first
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted");
            updateMessageStatusToFailed(messageId, "SMS permission not granted");
            return;
        }

        // Validate inputs
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Invalid phone number");
            updateMessageStatusToFailed(messageId, "Invalid phone number");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            Log.e(TAG, "Empty message");
            updateMessageStatusToFailed(messageId, "Empty message");
            return;
        }

        int deliveryRequestCode = (int) messageId + 5000;
        int pendingRequestCode = deliveryRequestCode + 1;

        PendingIntent sentPI = createPendingIntent(SMS_SENT_ACTION, messageId, pendingRequestCode);
        PendingIntent deliveredPI = createPendingIntent(SMS_DELIVERED_ACTION, messageId, deliveryRequestCode);

        registerReceivers();

        try {
            int smsToSendFrom = getSimCardId(simSlot);

            if (smsToSendFrom == -1) {
                Log.e(TAG, "Invalid SIM slot: " + simSlot);
                updateMessageStatusToFailed(messageId, "Invalid SIM slot");
                return;
            }

            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom);

            if (isMultipart) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentIntents = createIntentList(sentPI, parts.size());
                ArrayList<PendingIntent> deliveryIntents = createIntentList(deliveredPI, parts.size());
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
                Log.i(TAG, "Multipart SMS sent to " + phoneNumber + " via SIM slot " + simSlot);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
                Log.i(TAG, "SMS sent to " + phoneNumber + " via SIM slot " + simSlot);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
            updateMessageStatusToFailed(messageId, "SMS sending failed: " + e.getMessage());
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


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(sendBroadcastReceiver, new IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_EXPORTED);
            context.registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(sendBroadcastReceiver, new IntentFilter(SMS_SENT_ACTION));
            context.registerReceiver(deliveredBroadcastReceiver, new IntentFilter(SMS_DELIVERED_ACTION));
        }
    }

    private int getSimCardId(String simSlot) {
        try {
            ArrayList<Integer> simCardList = new ArrayList<>();
            SubscriptionManager subscriptionManager = SubscriptionManager.from(context);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
                    Log.e(TAG, "No active SIM subscriptions found");
                    return -1;
                }

                for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                    simCardList.add(subscriptionInfo.getSubscriptionId());
                }

                Log.i(TAG, "Available SIM slots: " + simCardList.size());

                int slotIndex;
                try {
                    slotIndex = simSlot.equals("1") ? 0 : 1;
                } catch (Exception e) {
                    Log.e(TAG, "Invalid simSlot format: " + simSlot);
                    return -1;
                }

                if (slotIndex >= simCardList.size()) {
                    Log.e(TAG, "SIM slot " + simSlot + " not available. Only " + simCardList.size() + " SIM(s) found");
                    return -1;
                }

                return simCardList.get(slotIndex);
            } else {
                Log.e(TAG, "READ_PHONE_STATE permission not granted");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM card ID: " + e.getMessage(), e);
            return -1;
        }
    }

    private void updateMessageStatusToFailed(long messageId, String errorMsg) {
        Log.e(TAG, "Updating message " + messageId + " to failed: " + errorMsg);
        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        // You can implement this method to update the message status via API
        // For now, we'll just log it
    }

    private ArrayList<PendingIntent> createIntentList(PendingIntent baseIntent, int size) {
        ArrayList<PendingIntent> intents = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            intents.add(baseIntent);
        }
        return intents;
    }

    /**
     * Test SMS sending capability with a small test message
     */
    private void testSmsSending() {
        Log.i(TAG, "Testing SMS sending capability...");

        try {
            // Send a very short test message to yourself
            SharedPrefManager manager = SharedPrefManager.getInstance(context);
            // You can implement this to send a test SMS to the device's own number
            // This helps detect if carrier is blocking

            Log.i(TAG, "SMS test initiated");
        } catch (Exception e) {
            Log.e(TAG, "SMS test failed: " + e.getMessage());
        }
    }

    /**
     * Detect common carrier blocking patterns
     */
    private boolean isCarrierLikelyBlocking(String errorCode, String carrierName) {
        // Common error codes that indicate carrier blocking
        String[] blockingErrorCodes = {
                "1", "2", "3", "4", "5", // Generic failure codes
                "RESULT_ERROR_GENERIC_FAILURE",
                "RESULT_ERROR_NO_SERVICE",
                "RESULT_ERROR_RADIO_OFF"
        };

        for (String code : blockingErrorCodes) {
            if (errorCode.contains(code)) {
                Log.w(TAG, "Possible carrier blocking detected for " + carrierName + " with error: " + errorCode);
                return true;
            }
        }

        return false;
    }

}
