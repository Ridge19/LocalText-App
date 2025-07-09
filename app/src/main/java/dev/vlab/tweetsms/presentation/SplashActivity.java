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
        // Always go to AccountLoginActivity (activity_auth.xml) on app start
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, AccountLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 500);
    }
}
