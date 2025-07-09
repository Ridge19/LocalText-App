package dev.vlab.tweetsms.presentation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import dev.vlab.tweetsms.R;
import dev.vlab.tweetsms.apiclient.ApiInterface;
import dev.vlab.tweetsms.apiclient.RetrofitInstance;
import dev.vlab.tweetsms.helper.SharedPrefManager;
import dev.vlab.tweetsms.helper.UrlContainer;
import dev.vlab.tweetsms.presentation.auth.AccountLoginActivity;
import dev.vlab.tweetsms.presentation.auth.ForgotPasswordActivity;
import dev.vlab.tweetsms.presentation.auth.LoginActivity;
import dev.vlab.tweetsms.presentation.auth.OtpActivity;
import dev.vlab.tweetsms.presentation.auth.ResetPasswordActivity;
import retrofit2.Retrofit;


public class SplashActivity extends AppCompatActivity {
    String TAG = "TAG";
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.splash_screen_bg));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        authMiddleware();

    }

    private void authMiddleware(){
        SharedPrefManager manager = SharedPrefManager.getInstance(SplashActivity.this);
        Retrofit retrofit = RetrofitInstance.getRetrofitInstance(UrlContainer.getBaseUrl());
        ApiInterface apiResponse = retrofit.create(ApiInterface.class);
     //  manager.setToken("");
        String token = manager.getToken();

        new Handler().postDelayed(()->{
            if (token == null || token.isEmpty() || token.contains("null")) {
                Intent intent = new Intent(SplashActivity.this, AccountLoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }else{
                Log.e("TOKEN -ELSE", token);
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        },1000);

    }
}
