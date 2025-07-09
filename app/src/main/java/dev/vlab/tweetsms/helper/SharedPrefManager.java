package dev.vlab.tweetsms.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static SharedPrefManager mInstance;
    private static Context mCtx;


    public static String LAST_TOKEN = "token";
    public static String LAST_TOKEN_KEY = "token_key";


    public static String DEVICE_ID = "device_id";
    public static String DEVICE_ID_KEY = "device_id_key";


    public static String BASE_URL = "base_url";
    public static String BASE_URL_KEY = "base_url_key";

    public static String PUSHER_KEY = "pusher_key";
    public static String PUSHER_KEY_KEY = "pusher_key_key";

    public static String PUSHER_ID = "pusher_id";
    public static String PUSHER_ID_KEY = "pusher_id_key";

    public static String PUSHER_SECRET = "pusher_secret";
    public static String PUSHER_SECRET_KEY = "pusher_secret_key";

    public static String PUSHER_CLUSTER = "pusher_cluster";
    public static String PUSHER_CLUSTER_KEY = "pusher_cluster_key";

    public static String QR_DATA = "qr_data";
    public static String QR_DATA_KEY = "qr_data_key";

    public static String STATUS = "status";
    public static String STATUS_KEY = "status_key";


    public SharedPrefManager(Context context) {
        mCtx = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefManager(context);
        }
        return mInstance;
    }

    public void setStatus(String status) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(STATUS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(STATUS_KEY, status);
        editor.apply();
    }

    public String getStatus() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(STATUS, Context.MODE_PRIVATE);
        return sharedPreferences.getString(STATUS_KEY, "null");
    }

    public void setQrData(String qrData) {

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(QR_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(QR_DATA_KEY, qrData);
        editor.apply();
    }

    public String getQrData() {

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(QR_DATA, Context.MODE_PRIVATE);
        return sharedPreferences.getString(QR_DATA_KEY, "null");
    }

    public void setDeviceId(String deviceId) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(DEVICE_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(DEVICE_ID_KEY, deviceId);
        editor.apply();
    }

    public String getDeviceId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(DEVICE_ID, Context.MODE_PRIVATE);
        return sharedPreferences.getString(DEVICE_ID_KEY, "null");
    }


    public void setBaseUrl(String baseUrl) {

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(BASE_URL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (baseUrl.endsWith("/")) {
            editor.putString(BASE_URL_KEY, baseUrl);
        } else {
            editor.putString(BASE_URL_KEY, baseUrl + '/');
        }
        editor.apply();


    }

    public String getBaseUrl() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(BASE_URL, Context.MODE_PRIVATE);
        return sharedPreferences.getString(BASE_URL_KEY, "null");
    }


    public void setToken(String name) {

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(LAST_TOKEN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(LAST_TOKEN_KEY, name);
        editor.apply();
    }

    public String getToken() {

        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(LAST_TOKEN, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(LAST_TOKEN_KEY, "null");

        return token;
    }


    public void setPusherKey(String pusherKey) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PUSHER_KEY_KEY, pusherKey);
        editor.apply();
    }

    public String getPusherKey() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_KEY, Context.MODE_PRIVATE);

        return sharedPreferences.getString(PUSHER_KEY_KEY, "null");
    }

    public void setPusherId(String pusherId) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PUSHER_ID_KEY, pusherId);
        editor.apply();
    }

    public String getPusherId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_ID, Context.MODE_PRIVATE);

        return sharedPreferences.getString(PUSHER_ID_KEY, "null");
    }

    public void setPusherSecret(String pusherSecret) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_SECRET, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PUSHER_SECRET_KEY, pusherSecret);
        editor.apply();
    }

    public String getPusherSecret() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_SECRET, Context.MODE_PRIVATE);

        return sharedPreferences.getString(PUSHER_SECRET_KEY, "null");
    }

    public void setPusherCluster(String cluster) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_CLUSTER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PUSHER_CLUSTER_KEY, cluster);
        editor.apply();
    }

    public String getPusherCluster() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PUSHER_CLUSTER, Context.MODE_PRIVATE);
        return sharedPreferences.getString(PUSHER_CLUSTER_KEY, "null");
    }

}
