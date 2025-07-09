package dev.vlab.tweetsms.presentation.auth;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;
import dev.vlab.tweetsms.helper.UrlContainer;
import dev.vlab.tweetsms.presentation.main.MainActivity;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    TextView email;
    EditText emailText;
    Button submit;
    ProgressBar progressBar;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        Window window = this.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView titleText = new TextView(this);
        progressBar = findViewById(R.id.progressBar);
        if (toolbar != null) {
            setSupportActionBar(toolbar); // ✅ Now this should work!
            getSupportActionBar().setTitle(R.string.Forgot_password);
            titleText.setTextColor(getResources().getColor(R.color.dark_gray));   // Change title color
            titleText.setTextSize(20);             // Change title size
            titleText.setTypeface(null, android.graphics.Typeface.BOLD); // Make text bold
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
        }

       // setSupportActionBar(toolbar);
        email = findViewById(R.id.eusername);
        submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = email.getText().toString().trim();

                if (username.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please Enter your username", Toast.LENGTH_SHORT).show();
                }else {
                    submit.setText("");
                    submit.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
                    ApiInterface apiResponse = retrofit.create(ApiInterface.class);

                    try {

                    Call<String> call =   apiResponse.sendForgotPasswordRequest(username);

                    call.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            assert response.body() != null;
                            Log.d("Success>>>",response.body().toString());
                            if (response.isSuccessful()) {
                                JSONObject object = null;
                                try {
                                    object = new JSONObject(response.body());
                                    String success = object.getString("status");

                                    if (success.equals("success")) {
                                        JSONObject userData = object.getJSONObject("data");
                                        Toasty.success(ForgotPasswordActivity.this, "Verification code sent to mail").show();

                                        submit.setText(R.string.login);
                                        submit.setEnabled(true);
                                        progressBar.setVisibility(View.GONE);

                                        Intent intent = new Intent(ForgotPasswordActivity.this, OtpActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                                        intent.putExtra("USERNAME",userData.getString("email")) ;
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                       // Log.d("SUCCED", "SUCCED >>>");
                                    } else {
                                        submit.setText(R.string.login);
                                        submit.setEnabled(true);
                                        progressBar.setVisibility(View.GONE);

                                        Toasty.error(ForgotPasswordActivity.this, "Error: " + object.getJSONArray("errors").toString(), Toast.LENGTH_SHORT).show();
                                    }

                                } catch (Exception e) {
                                    submit.setText(R.string.login);
                                    submit.setEnabled(true);
                                    progressBar.setVisibility(View.GONE);
                                    Log.e("EXCEPTION", e.getMessage());
                                    Toasty.error(ForgotPasswordActivity.this, "Unauthorized user", Toast.LENGTH_SHORT).show();

                                }
                            }else{
                                submit.setText(R.string.login);
                                submit.setEnabled(true);
                                progressBar.setVisibility(View.GONE);
                                Toasty.error(ForgotPasswordActivity.this, "Failed to login " + response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                                // Toasty.error(AccountLoginActivity.this, "Error: Request failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            Log.d("Error onFailure","call api");
                            submit.setText(R.string.login);
                            submit.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            Toasty.error(ForgotPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


                    } catch (Exception e) {
                        submit.setText(R.string.login);
                        submit.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ForgotPasswordActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.e("Toolbar", "Item selected: " + item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(ForgotPasswordActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
