package dev.vlab.tweetsms.presentation.auth;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
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

import org.json.JSONObject;


public class ResetPasswordActivity extends AppCompatActivity {


Button submit;
EditText password;
EditText confirmPassword;

 ProgressBar progressBar;
boolean isVisible;
ImageView eyeIcon;
ImageView eyeIconConfirm;

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        Window window = this.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        submit = findViewById(R.id.submit);
        progressBar = findViewById(R.id.progressBar);
        isVisible = false;
        eyeIcon = findViewById(R.id.eye_icon);
        eyeIconConfirm = findViewById(R.id.eye_iconConfirm);

        //info: toolbar added
       Toolbar toolbar = findViewById(R.id.toolbar);
        TextView titleText = new TextView(this);
        if (toolbar != null) {
            setSupportActionBar(toolbar); // ✅ Now this should work!
            getSupportActionBar().setTitle(R.string.reset_password);
            titleText.setTextColor(getResources().getColor(R.color.dark_gray));   // Change title color
            titleText.setTextSize(20);             // Change title size
            titleText.setTypeface(null, android.graphics.Typeface.BOLD); // Make text bold
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
        }



        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = getIntent().getStringExtra("USERNAME"); // Retrieve data
                Log.e("OTP_SCREEN", "Received username: " + username);

                String otp =  getIntent().getStringExtra("OTP");
                Log.e("OTP>>", otp);
                String  passwordTxt = password.getText().toString().trim();
                String  conPasswordTxt = confirmPassword.getText().toString().trim();
                if(!passwordTxt.equals(conPasswordTxt)){
                    Toasty.error(ResetPasswordActivity.this, "Confirm Password not match" ).show();
                }else {
                    submit.setText("");
                    submit.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);

                    Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
                    ApiInterface apiResponse = retrofit.create(ApiInterface.class);
                    Call<String> call =   apiResponse.resetPassword(username,otp,passwordTxt,conPasswordTxt);

                    try {


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
                                            Log.d("SUCCED", "SUCCED >>>");
                                            Toasty.success(ResetPasswordActivity.this, "Password changed successfully").show();
                                            Intent intent = new Intent(ResetPasswordActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                            submit.setText(R.string.login);
                                            submit.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            submit.setText(R.string.login);
                                            submit.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                            Toasty.error(ResetPasswordActivity.this, "Error: " + object.getJSONArray("errors").toString(), Toast.LENGTH_SHORT).show();
                                        }

                                    } catch (Exception e) {
                                        submit.setText(R.string.login);
                                        submit.setEnabled(true);
                                        progressBar.setVisibility(View.GONE);
                                        Log.e("EXCEPTION", e.getMessage());
                                        Toasty.error(ResetPasswordActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();

                                    }finally {
                                        Log.e("Finall", object.toString());
                                    }
                                }else{
                                    submit.setText(R.string.login);
                                    submit.setEnabled(true);
                                    progressBar.setVisibility(View.GONE);
                                    Toasty.error(ResetPasswordActivity.this, "Failed to login " + response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                                    // Toasty.error(AccountLoginActivity.this, "Error: Request failed", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                Log.d("Error onFailure","call api");
                                submit.setText(R.string.login);
                                submit.setEnabled(true);
                                progressBar.setVisibility(View.GONE);
                                Toasty.error(ResetPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });


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

        eyeIconConfirm.setOnClickListener(v -> {
            if (confirmPassword.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                eyeIconConfirm.setImageResource(R.drawable.eye_close); // Change icon if needed
            } else {
                confirmPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                eyeIconConfirm.setImageResource(R.drawable.eye); // Change icon if needed
            }
           confirmPassword.setSelection(confirmPassword.getText().length()); // Keep cursor at the end
        });


        // otp watcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ResetPasswordActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.e("Toolbar", "Item selected: " + item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(ResetPasswordActivity.this, AccountLoginActivity.class);         //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Dispose method to remove all listeners
    private void togglePasswordVisibility(EditText editText) {

        Drawable rightDrawable;
//        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
        if (isVisible) {
            isVisible= false;
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            rightDrawable =  getResources().getDrawable(R.drawable.eye);
        } else {
            isVisible= true;
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            rightDrawable = getResources().getDrawable(R.drawable.eye_close);
        }

        // Set the right drawable icon to reflect the change
        editText.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.lock), null, rightDrawable, null);
        editText.setSelection(editText.getText().length());
    }





}
