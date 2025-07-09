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
import android.content.pm.ActivityInfo;
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
import android.content.Intent;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;

import org.json.JSONObject;

public class AccountLoginActivity extends AppCompatActivity {

    EditText etEmailUsername;
    EditText password;

    Button btnLogin ;
    TextView forgot;

    ProgressBar progressBar ;

    ImageView eyeIcon;


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
                }else {
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
                                btnLogin.setText("Login"); // Restore button text
                                progressBar.setVisibility(View.GONE); // Hide loading indicator

                                String responseBody = response.body();
                                Log.d("Success>>>", "Response: " + (responseBody != null ? responseBody : "null body"));

                                if (response.isSuccessful() && responseBody != null) {
                                    JSONObject object = null;
                                    try {
                                        object = new JSONObject(responseBody);
                                        String success = object.getString("status");
                                        if (success.equals("success")) {
                                            JSONObject userData = object.getJSONObject("data");
                                            String accessToken = userData.getString("access_token");
                                            String tokenType = userData.getString("token_type");
                                            Log.d("accessToken>>>", accessToken);
                                            Toasty.success(AccountLoginActivity.this, "Login Success").show();

                                            //pusher json
                                            JSONObject pusherObj = userData.getJSONObject("pusher");
                                            String pusherKey = pusherObj.getString("pusher_key");
                                            String pusherId = pusherObj.getString("pusher_id");
                                            String pusherSecretId = pusherObj.getString("pusher_secret");
                                            String pusherClusterId = pusherObj.getString("pusher_cluster");

                                            Toasty.success(AccountLoginActivity.this, "Login Success").show();
                                            SharedPrefManager manager = SharedPrefManager.getInstance(AccountLoginActivity.this);
                                            manager.setToken(tokenType + " " + accessToken);
                                            Log.i("Token",tokenType + " " + accessToken);
                                            //pusher data
                                            manager.setPusherKey(pusherKey);
                                            manager.setPusherId(pusherId);
                                            manager.setPusherSecret(pusherSecretId);
                                            manager.setPusherCluster(pusherClusterId);

                                            //save base url
                                            manager.setBaseUrl(UrlContainer.getBaseUrl());
                                            btnLogin.setText(R.string.login);
                                            btnLogin.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                            Intent intent = new Intent(AccountLoginActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();

                                        } else {
                                            btnLogin.setText(R.string.login);
                                            btnLogin.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                            Toasty.error(AccountLoginActivity.this, "Error: " + object.getJSONArray("errors").toString(), Toast.LENGTH_SHORT).show();
                                        }

                                    } catch (Exception e) {
                                        btnLogin.setText(R.string.login);
                                        btnLogin.setEnabled(true);
                                        progressBar.setVisibility(View.GONE);
                                        Log.e("EXCEPTION", e.getMessage());
                                        Toasty.error(AccountLoginActivity.this, "Unauthorized user", Toast.LENGTH_SHORT).show();

                                    }
                                } else {
                                    btnLogin.setText(R.string.login);
                                    btnLogin.setEnabled(true);
                                    progressBar.setVisibility(View.GONE);

                                    String errorMsg = "Failed to login";
                                    if (response.errorBody() != null) {
                                        try {
                                            errorMsg = response.errorBody().string();
                                        } catch (Exception e) {
                                            Log.e("ERROR", "Error reading error body: " + e.getMessage());
                                        }
                                    }
                                    Toasty.error(AccountLoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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
