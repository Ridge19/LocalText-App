package dev.vlab.tweetsms.presentation.auth;
import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;
import dev.vlab.tweetsms.helper.UrlContainer;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccountLoginActivity extends AppCompatActivity {

    EditText etEmailUsername;
    EditText password;

    Button btnLogin ;
    TextView forgot;

    ProgressBar progressBar ;

    ImageView eyeIcon;
    CheckBox rememberMeCheckBox;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etEmailUsername = findViewById(R.id.eusername);
        password = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.submit);
        forgot = findViewById(R.id.routeForgot);
        eyeIcon =  findViewById(R.id.eye_icon);
        progressBar = findViewById(R.id.progressBar);
        rememberMeCheckBox = findViewById(R.id.checkbox_remember_me);

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        // Restore saved credentials if Remember Me was checked
        if (prefs.getBoolean("remember_me", false)) {
            etEmailUsername.setText(prefs.getString("username", ""));
            password.setText(prefs.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }

        //
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.splash_screen_bg));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        eyeIcon.setOnClickListener(v -> {
            if (password.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eyeIcon.setImageResource(R.drawable.eye_close); // Change icon if needed
            } else {
                password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eyeIcon.setImageResource(R.drawable.eye); // Change icon if needed
            }
            password.setSelection(password.getText().length()); // Keep cursor at the end
        });

        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccountLoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputEmailUsername = etEmailUsername.getText().toString().trim();
                String inputPassword = password.getText().toString().trim();
                if (inputEmailUsername.isEmpty() || inputPassword.isEmpty()) {
                    Toast.makeText(AccountLoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Save credentials if Remember Me is checked
                    if (rememberMeCheckBox.isChecked()) {
                        prefs.edit()
                            .putBoolean("remember_me", true)
                            .putString("username", inputEmailUsername)
                            .putString("password", inputPassword)
                            .apply();
                    } else {
                        prefs.edit().clear().apply();
                    }

                    btnLogin.setEnabled(false);
                    btnLogin.setText(""); // Hide button text
                    progressBar.setVisibility(View.VISIBLE); // Show loading indicator
                    Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
                    ApiInterface apiResponse = retrofit.create(ApiInterface.class);
                    try {
                        Call<String> call = apiResponse.sendLoginRequest(inputEmailUsername, inputPassword);
                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                btnLogin.setEnabled(true);
                                btnLogin.setText("Login");
                                progressBar.setVisibility(View.GONE);

                                String responseBody = response.body();
                                Log.d("API_Response", "Response: " + (responseBody != null ? responseBody : "null body"));

                                if (!response.isSuccessful() || responseBody == null) {
                                    String errorMsg = "Login failed. Please try again.";
                                    if (response.errorBody() != null) {
                                        try {
                                            String errorBodyStr = response.errorBody().string();
                                            if (errorBodyStr.toLowerCase().contains("invalid") || errorBodyStr.toLowerCase().contains("incorrect")) {
                                                errorMsg = "Incorrect email or password.";
                                            }
                                        } catch (Exception e) {
                                            // ignore, use default errorMsg
                                        }
                                    }
                                    Toasty.error(AccountLoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (response.isSuccessful() && responseBody != null) {
                                    JSONObject object = null;
                                    try {
                                        object = new JSONObject(responseBody);
                                        String success = object.optString("status", "");
                                        if (success.equals("success")) {

                                            JSONObject userData = object.optJSONObject("data");
                                            if (userData == null) {
                                                Toasty.error(AccountLoginActivity.this, "Error: Missing user data", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            String accessToken = userData.optString("access_token", null);
                                            String tokenType = userData.optString("token_type", null);

                                            if (accessToken == null || tokenType == null) {
                                                Toasty.error(AccountLoginActivity.this, "Login failed: Missing token data", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            SharedPrefManager manager = SharedPrefManager.getInstance(AccountLoginActivity.this);
                                            manager.setToken(tokenType + " " + accessToken);
                                            manager.setBaseUrl(UrlContainer.getBaseUrl());

                                            // Optional: Log token
                                            Log.d("accessToken>>>", tokenType + " " + accessToken);

                                            // Handle pusher details
                                            JSONObject pusherObj = userData.optJSONObject("pusher");
                                            if (pusherObj != null) {
                                                manager.setPusherKey(pusherObj.optString("pusher_key", null));
                                                manager.setPusherId(pusherObj.optString("pusher_id", null));
                                                manager.setPusherSecret(pusherObj.optString("pusher_secret", null));
                                                manager.setPusherCluster(pusherObj.optString("pusher_cluster", null));
                                            }

                                            Toasty.success(AccountLoginActivity.this, "Login Success").show();

                                            Intent intent = new Intent(AccountLoginActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                            return;
                                        }
                                    } catch (Exception e) {
                                        Log.e("API_Exception", "Exception while parsing response", e);
                                        Toasty.error(AccountLoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                Log.d("Error onFailure", "call api");
                                btnLogin.setText(R.string.login);
                                btnLogin.setEnabled(true);
                                progressBar.setVisibility(View.GONE);
                                Toasty.error(AccountLoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        btnLogin.setText(R.string.login);
                        btnLogin.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        Log.d("error from exception", e.getMessage());
                        Toast.makeText(AccountLoginActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        rememberMeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                // User unticked Remember Me: clear connection (QR data)
                SharedPrefManager manager = SharedPrefManager.getInstance(AccountLoginActivity.this);
                manager.setQrData("");
                prefs.edit().remove("remember_me").remove("username").remove("password").apply();
            }
            // If checked, do nothing here; credentials will be saved on login
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show AlertDialog when the back button is pressed
                showExitDialog();
            }
        });

    }


    private void showExitDialog() {
        // Create the AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)  // Prevent closing by tapping outside the dialog
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //   AccountLoginActivity.super.getOnBackPressedDispatcher(); // Perform the default back button action
                        finish();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle the negative button click (do nothing and dismiss dialog)
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private String getActionString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return "ACTION_DOWN";
            case MotionEvent.ACTION_UP:
                return "ACTION_UP";
            case MotionEvent.ACTION_MOVE:
                return "ACTION_MOVE";
            case MotionEvent.ACTION_CANCEL:
                return "ACTION_CANCEL";
            default:
                return "UNKNOWN_ACTION";
        }
    }
}
