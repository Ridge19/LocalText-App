package dev.vlab.tweetsms.helper;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.util.HttpChannelAuthorizer;

import java.util.HashMap;
import java.util.Map;

public class PusherOdk {

    private static PusherOdk instance;
    private final Pusher pusherApp;
    public static boolean isConnected = false;

    // Private constructor to prevent instantiation
    private PusherOdk(String baseUrl, String channelKey, String cluster, String token) {
        String endpoint = baseUrl + "pusher/auth";
        final HttpChannelAuthorizer authorizer = new HttpChannelAuthorizer(endpoint);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);

        authorizer.setHeaders(headers);

        PusherOptions pusherOptions = new PusherOptions()
                .setUseTLS(true)
                .setCluster(cluster)
                .setChannelAuthorizer(authorizer);
        pusherApp = new Pusher(channelKey, pusherOptions);
    }

    // Synchronized method to control simultaneous access
    public static synchronized PusherOdk getInstance(String baseUrl, String channelKey, String cluster, String token) {
        if (instance == null) {
            instance = new PusherOdk(baseUrl, channelKey, cluster, token);
        }
        return instance;
    }

    // Ensures the singleton instance is reset with new parameters
    public static synchronized void resetInstance(String baseUrl, String channelKey, String cluster, String token) {
        if (instance != null) {
            instance.disconnect(); // Ensure the current instance is disconnected
        }
        instance = new PusherOdk(baseUrl, channelKey, cluster, token);
    }

    public void connect() {
        if (!isConnected) {
            pusherApp.connect();
            isConnected = true;
        }
    }

    public void disconnect() {
        if (isConnected) {
            pusherApp.disconnect();
            isConnected = false;
        }
    }

    public Pusher getPusherApp() {
        return pusherApp;
    }
}
