package dev.vlab.tweetsms.presentation.auth;


import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.helper.UrlContainer;
import dev.vlab.tweetsms.presentation.main.MainActivity;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    LinearLayout qrBtn;
    boolean isLoading = false;

    String TAG = "TAG";
    String deviceIMEI = "", deviceName, androidVersion, appVersion;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //  window.setStatusBarColor(getResources().getColor(R.color.splash_screen_bg));
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.splash_screen_bg));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.splash_screen_bg));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        CheckPermissionAndStartIntent();
        getSimInfo();
        setDeviceInfoData();


        SharedPrefManager manager = SharedPrefManager.getInstance(LoginActivity.this);
        String qrCode = manager.getQrData();
        Log.i(TAG, "QRDATA   " + qrCode);
        if (qrCode != null && !qrCode.equals("null") && qrCode.length() > 5) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return; // Prevent further execution
        }

        qrBtn = findViewById(R.id.qr_fab);
        qrBtn.setOnClickListener(view -> openQrScanner());

    }


    private void setDeviceIdToSharedPref(String deviceIMEI) {
        SharedPrefManager manager = SharedPrefManager.getInstance(LoginActivity.this);
        manager.setDeviceId(deviceIMEI);
    }


    private void loginUserWithQr(String scannedData) {

        String serverUrl = "";
        try {
            serverUrl = scannedData.split("HOST")[1];
        } catch (Exception e) {
            Log.e(TAG, "exception: " + e.toString());
            return;
        }


        Log.e("serverUrl:", scannedData);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please Wait");
        progressDialog.show();

        SharedPrefManager manager = SharedPrefManager.getInstance(LoginActivity.this);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);
        String token = manager.getToken();
        String simInfo = getSimInfo();
        Log.i("token>>>",token);
        Log.i("token>>>",scannedData);
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
                progressDialog.dismiss();

                Log.e("EXCEPTION",response.toString() );

                if (response.isSuccessful() && response.body() != null) {
                    try {

                        JSONObject object = new JSONObject(response.body());
                        boolean success = object.getBoolean("success");

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

                            Toasty.success(LoginActivity.this, "Login Success").show();
                            SharedPrefManager manager = SharedPrefManager.getInstance(LoginActivity.this);
                            manager.setToken(tokenType + " " + accessToken);
                            manager.setQrData(scannedData);
                            manager.setStatus("true");

                            //pusher data

                            manager.setPusherKey(pusherKey);
                            manager.setPusherId(pusherId);
                            manager.setPusherSecret(pusherSecretId);
                            manager.setPusherCluster(pusherClusterId);

                            //save base url
                            manager.setBaseUrl(UrlContainer.getBaseUrl());
//                            manager.setBaseUrl(baseUrl);

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            Log.e("onFailure", "onResponse: " + object);
                            String errorMsg = "Login failed";
                            try {
                                if (object.has("errors")) {
                                    errorMsg = object.getJSONArray("errors").toString();
                                } else if (object.has("message")) {
                                    errorMsg = object.getString("message");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error message: " + e.getMessage());
                            }
                            Toasty.error(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "onResponse JSON parsing error: " + e.toString());
                        Toasty.error(LoginActivity.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Failed to login";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                    }
                    Toasty.error(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {

                Toasty.error(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });

    }

    private String getSimInfo() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SIM-PERMISSION>>>", "NOT Granted");
            return ""; // Return empty string if permission is not granted
        }

        try {
            String simName = "";
            SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
            if (subscriptionManager == null) {
                Log.e("SIM-PERMISSION>>>", "SubscriptionManager is null");
                return "";
            }

            List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
                Log.e("SIM-PERMISSION>>>", "No active subscriptions found.");
                return ""; // Return empty string if no subscriptions are found
            }

            Log.e("SIM-PERMISSION>>>","Size: " + subscriptionInfos.size());

            for (int i = 0; i < subscriptionInfos.size(); i++) {
                SubscriptionInfo subInfo = subscriptionInfos.get(i);
                if (subInfo == null) continue;

                Log.e("SIM-PERMISSION>>>", subInfo.getCountryIso());
                if (i == 0) {
                    simName = subInfo.getCarrierName() + "";
                } else if (i == 1) {
                    simName = simName + "," + subInfo.getCarrierName();
                }
            }
            Log.e("SIM-PERMISSION>>>simName", simName);
            return simName;
        } catch (Exception e) {
            Log.e("SIM-PERMISSION>>>", "Error getting SIM info: " + e.getMessage());
            return "";
        }
    }


    void openQrScanner() {
        ScanOptions options = new ScanOptions();

        options.setPrompt("Scan something");
        options.setOrientationLocked(true);
        options.setBeepEnabled(false);
        barcodeLauncher.launch(options);


    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Intent originalIntent = result.getOriginalIntent();
                    if (originalIntent == null) {
                        Toast.makeText(LoginActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Toast.makeText(LoginActivity.this, "Cancelled due to missing camera permission", Toast.LENGTH_LONG).show();
                    }
                } else {
                    isLoading = true;
                    setDeviceInfoData();
                    if (result.getContents().length() < 12) {
                        isLoading = false;
                        Toasty.error(getApplicationContext(), "Something went wrong , Please try again", Toasty.LENGTH_LONG, true).show();
                    } else {
                        loginUserWithQr(result.getContents());
                    }

                }
            });

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

    private void CheckPermissionAndStartIntent() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(LoginActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(LoginActivity.this, permissions, 1);
        } else {
            setDeviceInfoData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                setDeviceInfoData();
            } else {
                Toasty.error(this, "Required permissions not granted. App may not work properly.", Toast.LENGTH_LONG).show();
                // Still proceed but with limited functionality
                setDeviceInfoData();
            }
        }
    }

}