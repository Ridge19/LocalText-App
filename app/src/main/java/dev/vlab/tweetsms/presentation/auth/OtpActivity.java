package dev.vlab.tweetsms.presentation.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
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
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar; // ✅ Correct
import android.os.Bundle;

import org.json.JSONObject;

public class OtpActivity extends AppCompatActivity {

    private EditText[] otpFields;
    private TextWatcher[] otpTextWatchers;
    Button submit;
    TextView subTitle;
    TextView resendAgain;
    ProgressBar progressBar;
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        Window window = this.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        String username = getIntent().getStringExtra("USERNAME"); // Retrieve data
        submit = findViewById(R.id.submit);
        subTitle = findViewById(R.id.email_txt);
        resendAgain = findViewById(R.id.resendAgain);
        String message = getString(R.string.password_verification_sub) + " " + username;
        subTitle.setText(message);
        progressBar =  findViewById(R.id.progressBar);

        otpFields = new EditText[] {
                findViewById(R.id.otp_digit_1),
                findViewById(R.id.otp_digit_2),
                findViewById(R.id.otp_digit_3),
                findViewById(R.id.otp_digit_4),
                findViewById(R.id.otp_digit_5),
                findViewById(R.id.otp_digit_6)
        };
        otpTextWatchers = new TextWatcher[otpFields.length]; // Initialize the array to store TextWatchers

        //info: toolbar added
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView titleText = new TextView(this);
        if (toolbar != null) {
            setSupportActionBar(toolbar); // ✅ Now this should work!
            getSupportActionBar().setTitle(R.string.password_verification);
            titleText.setTextColor(getResources().getColor(R.color.dark_gray));   // Change title color
            titleText.setTextSize(20);             // Change title size
            titleText.setTypeface(null, android.graphics.Typeface.BOLD); // Make text bold
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
        }



        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("OTP_SCREEN", "Received username: " + username);

                String otp = getOtpValue();
                Log.e("OTP>>", otp);
                if(otp.isEmpty() || otp.length() < 6){
                    Toasty.error(OtpActivity.this, "Enter your OTP from  " + username).show();
                }else {
                    submit.setEnabled(false);
                    submit.setText(""); // Hide button text
                    progressBar.setVisibility(View.VISIBLE); // Show loading indicator
                    Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
                    ApiInterface apiResponse = retrofit.create(ApiInterface.class);
                    try {
                        Call<String> call =   apiResponse.otpVerificationRequest(username,otp);

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
                                            submit.setEnabled(true);
                                            submit.setText(R.string.verify); // Hide button text
                                            progressBar.setVisibility(View.GONE); // Show loading indicator
                                            Toasty.success(OtpActivity.this, "OTP match successfully").show();
                                            Intent intent = new Intent(OtpActivity.this, ResetPasswordActivity.class);
                                            intent.putExtra("USERNAME",username) ;
                                            intent.putExtra("OTP",otp) ;
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();

                                        } else {
                                            submit.setEnabled(true);
                                            submit.setText(R.string.verify); // Hide button text
                                            progressBar.setVisibility(View.GONE); // Show loading indicator
                                            Toasty.error(OtpActivity.this, "Error: " + object.getJSONArray("errors").toString(), Toast.LENGTH_SHORT).show();
                                        }

                                    } catch (Exception e) {
                                        submit.setEnabled(true);
                                        submit.setText(R.string.verify); // Hide button text
                                        progressBar.setVisibility(View.GONE); // Show loading indicator
                                        Log.e("EXCEPTION", e.getMessage());
                                        Toasty.error(OtpActivity.this, "Verification code doesn't match", Toast.LENGTH_SHORT).show();

                                    }
                                }else{
                                    submit.setEnabled(true);
                                    submit.setText(R.string.verify); // Hide button text
                                    progressBar.setVisibility(View.GONE); // Show loading indicator
                                    Toasty.error(OtpActivity.this, "Failed to login " + response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                                    // Toasty.error(AccountLoginActivity.this, "Error: Request failed", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                Log.d("Error onFailure","call api");
                                submit.setEnabled(true);
                                submit.setText(R.string.verify); // Hide button text
                                progressBar.setVisibility(View.GONE); // Show loading indicator
                                Toasty.error(OtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        submit.setEnabled(true);
                        submit.setText(R.string.verify); // Hide button text
                        progressBar.setVisibility(View.GONE); // Show loading indicator
                        throw new RuntimeException(e);
                    }
//
                }
            }
        });

        // resend otp code again
        resendAgain.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                runOnUiThread(() -> resendAgain.setText(R.string.sending));

                Log.e("Resend",  resendAgain.getText().toString());

                Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());

                    ApiInterface apiResponse = retrofit.create(ApiInterface.class);
                    resendAgain.setText(R.string.sending);
                    try {
                        Call<String> call =   apiResponse.sendForgotPasswordRequest(username);

                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {

                                assert response.body() != null;

                                if (response.isSuccessful()) {
                                    JSONObject object = null;
                                    try {
                                        object = new JSONObject(response.body());
                                        String success = object.getString("status");

                                        if (success.equals("success")) {

                                            Toasty.success(OtpActivity.this, "OTP send successfully").show();

                                        } else {
                                            Toasty.error(OtpActivity.this, "Error: " + object.getJSONArray("errors").toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        resendAgain.setText(R.string.resendCode);
                                    } catch (Exception e) {
                                        resendAgain.setText(R.string.resendCode);
                                        Log.e("EXCEPTION", e.getMessage());
                                        Toasty.error(OtpActivity.this, "Verification code doesn't match", Toast.LENGTH_SHORT).show();

                                    }finally {
                                        Log.e("Finally", object.toString());
                                    }
                                }else{
                                    Toasty.error(OtpActivity.this, "Failed to login " + response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                                    // Toasty.error(AccountLoginActivity.this, "Error: Request failed", Toast.LENGTH_SHORT).show();
                                    resendAgain.setText(R.string.resendCode);
                                }

                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                Log.d("Error onFailure","call api");
                                resendAgain.setText(R.string.resendCode);
                                Toasty.error(OtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        resendAgain.setText(R.string.resendCode);
                        throw new RuntimeException(e);
                    }
                Log.e("Resend",  resendAgain.getText().toString());


                Log.e("Resend",  resendAgain.getText().toString());

            }
        });


        // otp watcher
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpTextWatchers[i] = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    // Not needed for this case
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    // Move to next EditText on input
                    if (charSequence.length() == 1) {
                        if (index < otpFields.length - 1) {
                            otpFields[index + 1].requestFocus();
                        }
                    } else if (charSequence.length() == 0) {
                        // Move to previous EditText if user deletes a digit
                        if (index > 0) {
                            otpFields[index - 1].requestFocus();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    // After the text has changed, you can add any additional behavior if necessary
                    // In this case, we don't need to do anything here
                }
            };
            otpFields[i].addTextChangedListener(otpTextWatchers[i]); // Add the TextWatcher to the EditText
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(OtpActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.e("Toolbar", "Item selected: " + item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(OtpActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Dispose method to remove all listeners
    public void dispose() {
        for (int i = 0; i < otpFields.length; i++) {
            if (otpTextWatchers[i] != null) {
                otpFields[i].removeTextChangedListener(otpTextWatchers[i]); // Remove the listener
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose(); // Call the dispose method when the activity is destroyed to remove listeners
    }


    private String getOtpValue() {
        StringBuilder otpValue = new StringBuilder();
        for (EditText otpField : otpFields) {
            otpValue.append(otpField.getText().toString().trim());
        }
        return otpValue.toString();
    }

}
