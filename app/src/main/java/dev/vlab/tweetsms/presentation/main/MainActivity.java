package dev.vlab.tweetsms.presentation.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.helper.PusherOdk;
import dev.vlab.tweetsms.helper.UrlContainer;
import dev.vlab.tweetsms.presentation.auth.AccountLoginActivity;
import dev.vlab.tweetsms.presentation.auth.LoginActivity;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.service.SmsWorkManager;
import dev.vlab.tweetsms.helper.SharedPrefManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {
    Context context;
    String TAG = "TAG";
    public static final String SMS_SENT_ACTION = "dev.vlab.tweetsms.SMS_SENT_ACTION";
    public static final String SMS_DELIVERED_ACTION = "dev.vlab.tweetsms.SMS_DELIVERED_ACTION";
    WorkManager mWorkManager;

    Context ctx;

    TextView statusText;
    Button logoutButton;
    LinearLayout connectedLL;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint({"QueryPermissionsNeeded", "SetTextI18n", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getApplicationContext();
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        //      window.setNavigationBarColor(getResources().getColor(R.color.splash_screen_bg));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.splash_screen_bg));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.splash_screen_bg));
        }

        statusText = findViewById(R.id.status_text_id);
        connectedLL = findViewById(R.id.connected_ll);
        logoutButton = findViewById(R.id.logout);
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable = getResources().getDrawable(R.drawable.logout);
        drawable.setBounds(0, 0, 30, 30); // Set the size here
        logoutButton.setCompoundDrawables(drawable, null, null, null);

        setInitialColor();

        connectedLL.setOnClickListener(view -> {

            if (statusText.getText().toString().equalsIgnoreCase(connecting) || statusText.getText().toString().equalsIgnoreCase(disconnecting)) {

            } else {
                changeOnOffStatus();

            }

        });

        logoutButton.setOnClickListener(v -> {
            // Create a confirmation dialog
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Perform logout actions
                        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
                        manager.setStatus("false");
                        logoutButton.setText("Logout....");
                        logout();
                        // Don't set text back to "Logout" here, as the activity will finish or change
                        // Optionally, you can disable the button to prevent double clicks
                        logoutButton.setEnabled(false);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Dismiss the dialog
                        dialog.dismiss();
                    })
                    .show();
        });


        requestPermissions(new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.POST_NOTIFICATIONS,
        }, 0);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");

        try {
            // Check if WorkManager is available
            if (mWorkManager == null) {
                // Initialize mWorkManager
                mWorkManager = WorkManager.getInstance(this);
            }

            // Cancel all previous work
            mWorkManager.cancelAllWork();

            // Call Pusher with error handling
            setupBackgroundService();
        } catch (Exception e) {
            Log.e(TAG, "onStart: error " + e.getMessage(), e);
            // Show error to user but don't crash
            if (statusText != null) {
                statusText.setText("Error starting services");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    void setInitialColor() {


        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        String status = manager.getStatus();
        if (status.equalsIgnoreCase("true")) {

            connectedLL.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_connect));
            statusText.setText("Connected");

        } else {
            connectedLL.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_disconnect));
            statusText.setText("Disconnected");
        }


    }

    private void changeOnOffStatus() {

        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        String status = manager.getStatus();
        if (status.equalsIgnoreCase("true")) {
            logout();
        } else {
            loginUserWithQr(manager.getQrData());
            manager.setStatus("true");
        }
    }

    String connecting = "Connecting...";
    String disconnecting = "Disconnecting...";

    private void loginUserWithQr(String scannedData) {

        statusText.setText(connecting);
        setDeviceInfoData();
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);
        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        String token = manager.getToken();
        String simInfo = getSimInfo();
        Log.i("token>>>",token);
        Log.i("token>>>",deviceIMEI);
        Log.i("token>>>",deviceName);
        Log.i("token>>>",getDeviceModel());
        Log.i("token>>>",androidVersion);
        Log.i("token>>>",appVersion);
        Log.i("token>>>",simInfo);
        Call<String> call = apiResponse.sendLoginRequestWithQr(token,scannedData, deviceIMEI, deviceName, getDeviceModel(), androidVersion, appVersion, simInfo);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.e(TAG, "onResponse: connection" + response.body());
                Log.e(TAG, "SIM LIST:" + simInfo);
                Log.e(TAG, "IMEI " + deviceIMEI + " device name " + deviceName + " android version " + androidVersion + "--- " + appVersion);
                Log.e(TAG, "onResponse: error body" + response.errorBody());
                Log.e(TAG, "onResponse: code " + response.code());
                if (response.isSuccessful()) {
                    try {

                        JSONObject object = null;
                        if (response.body() != null) {
                            object = new JSONObject(response.body());
                        }

                        boolean success = false;
                        if (object != null) {
                            success = object.getBoolean("success");
                        }

                        if (success) {

                            JSONObject userData = object.getJSONObject("data");
                            String accessToken = userData.getString("access_token");
                            String tokenType = userData.getString("token_type");
                            String baseUrl = userData.getString("base_url");

                            //pusher json

                            JSONObject pusherObj = userData.getJSONObject("pusher");
                            String pusherKey = pusherObj.getString("pusher_key");
                            String pusherId = pusherObj.getString("pusher_id");
                            String pusherSecretId = pusherObj.getString("pusher_secret");
                            String pusherClusterId = pusherObj.getString("pusher_cluster");
                            SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
                            manager.setToken(tokenType + " " + accessToken);
                            //pusher data
                            manager.setPusherKey(pusherKey);
                            manager.setPusherId(pusherId);
                            manager.setPusherSecret(pusherSecretId);
                            manager.setPusherCluster(pusherClusterId);

                            //save base url
                            manager.setBaseUrl(UrlContainer.getBaseUrl());

                            Log.e(TAG, "onResponse: " + UrlContainer.getBaseUrl());
                            Log.e(TAG, "pusher key: " + manager.getPusherKey());
                            Log.e(TAG, "pusher id: " + manager.getPusherCluster());
                            Log.e(TAG, "pusher secret: " + manager.getPusherSecret());
                            Log.e(TAG, "pusher cluster: " + manager.getPusherId());
                            connectDevice(false);

                        } else {
                            connectDevice(true);
                            if (object != null) {

                                Toasty.error(MainActivity.this, "Error: " + object.getJSONArray("errors"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        connectDevice(true);
                    }
                } else {
                    Toasty.error(MainActivity.this, "Connection failed for " + response.errorBody(), Toast.LENGTH_SHORT).show();
                    connectDevice(true);
                }

            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(TAG, "connection failed for : " + t.getMessage());
                Toasty.error(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                connectDevice(true);
            }
        });

    }

    void gotoLoginActivity() {
        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        manager.setQrData("");
        Toasty.success(MainActivity.this, "Logout successful", Toast.LENGTH_SHORT, true).show();
        startActivity(new Intent(MainActivity.this, AccountLoginActivity.class));
        finish();
    }

    @SuppressLint("SetTextI18n")
    void connectDevice(boolean isError) {
        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        if (isError) {
            manager.setStatus("false");
            gotoLoginActivity();
        }
        setupBackgroundService();
        connectedLL.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_connect));
        statusText.setText("Connected");

    }

    private void restartActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    String deviceIMEI = "", deviceName, androidVersion, appVersion;

    private void setDeviceInfoData() {
        @SuppressLint("HardwareIds") String id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        deviceIMEI = id;
        deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
        androidVersion = Build.VERSION.RELEASE;
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String getDeviceModel() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private String getSimInfo() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }

        try {
            String simName = "";
            SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
            if (subscriptionManager == null) {
                return "";
            }

            List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
                return "";
            }

            for (int i = 0; i < subscriptionInfos.size(); i++) {
                SubscriptionInfo subInfo = subscriptionInfos.get(i);
                if (subInfo == null) continue;

                if (i == 0) {
                    simName = subInfo.getCarrierName() + "";
                } else if (i == 1) {
                    simName = simName + "," + subInfo.getCarrierName();
                }
            }
            return simName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM info: " + e.getMessage());
            return "";
        }
    }

    void setupBackgroundService() {
        Log.e(TAG, "setupBackgroundService: CALLED");
        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        String status = manager.getStatus();

        if (status.equalsIgnoreCase("true")) {
            Log.d(TAG, "callPusher: Enqueuing SmsWorkManager task");

            try {
                // Create a OneTimeWorkRequest for SmsWorkManager
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsWorkManager.class)
                        .build();

                // Enqueue the work request
                mWorkManager.enqueue(request).getResult()
                        .addListener(() -> Log.d(TAG, "SmsWorkManager work enqueued successfully"),
                                ContextCompat.getMainExecutor(this));

                Log.d(TAG, "callPusher: SmsWorkManager work enqueued");

            } catch (Exception e) {
                // Handle exceptions that might occur during enqueuing
                Log.e(TAG, "Error enqueuing SmsWorkManager task", e);
            }
        }
    }

    private void logout() {

        statusText.setText(disconnecting);
        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);

        String deviceId = manager.getDeviceId();
        String token = manager.getToken();


        try {
            Call<String> call = apiResponse.sendLogOutRequest(token, deviceId);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {

                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        Log.i(TAG, response.body());
                        logoutAction(false);
                    } else {
                        logoutAction(true);
                    }

                }

                @SuppressLint("CheckResult")
                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    Toasty.error(MainActivity.this, "Something Went Wrong");
                    logoutAction(true);
                }
            });
        } catch (Exception e) {
            logoutAction(true);
        }


    }

    @SuppressLint("SetTextI18n")
    void logoutAction(boolean isError) {

        SharedPrefManager manager = SharedPrefManager.getInstance(MainActivity.this);
        String status = manager.getStatus();

        if (isError) {
            manager.setStatus("false");
            gotoLoginActivity();

        } else {


            manager.setStatus("false");

            connectedLL.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.bg_disconnect));

            statusText.setText("Disconnected");
//            restartActivity();
            disConnectPusher();

        }

    }

    private void disConnectPusher() {
        // Implement your Pusher disconnection logic here
        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        PusherOdk pusherOdk = PusherOdk.getInstance(UrlContainer.getBaseUrl(), manager.getPusherKey(), manager.getPusherCluster(), manager.getToken());
        pusherOdk.disconnect();
        Log.i(TAG, "Subscription DISCONNECTED");
    }

    private void reConnectPusher() {
        // Implement your Pusher disconnection logic here
        SharedPrefManager manager = SharedPrefManager.getInstance(context);
        PusherOdk pusherOdk = PusherOdk.getInstance(UrlContainer.getBaseUrl(), manager.getPusherKey(), manager.getPusherCluster(), manager.getToken());
        pusherOdk.disconnect();
        Log.i(TAG, "Subscription DISCONNECTED");
    }

}
