package com.example.networkdetection.demo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class InternetConnectionStatusHelper {
    private static InternetConnectionStatusHelper instance;
    private final Context context;
    private final List<InternetConnectionListener> listeners;
    private boolean isChecking;
    private int currentRetryCount;

    private InternetConnectionStatusHelper(Context context) {
        this.context = context.getApplicationContext();
        listeners = new ArrayList<>();
    }

    public static synchronized InternetConnectionStatusHelper getInstance(Context context) {
        if (instance == null) {
            instance = new InternetConnectionStatusHelper(context);
        }
        return instance;
    }

    public interface InternetConnectionListener {
        void onConnectionOn();
        void onConnectOff();
    }

    public boolean getCurrentInternetConnectionStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void addInternetConnectionListener(InternetConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeInternetConnectionListener(InternetConnectionListener listener) {
        listeners.remove(listener);
    }

    public void triggerInternetConnectionStatusCheck() {
        if (isChecking) {
            // Check is already in progress, do nothing
            return;
        }

        isChecking = true;
        currentRetryCount = 0;

        // Start the checking process
        checkInternetConnection();
    }

    private void checkInternetConnection() {
        if (currentRetryCount >= 3) {
            // Reached maximum retries, notify listeners and reset
            isChecking = false;
            currentRetryCount = 0;
            notifyConnectOff();
            return;
        }

        if (getCurrentInternetConnectionStatus()) {
            // Internet is connected, notify listeners and reset
            isChecking = false;
            currentRetryCount = 0;
            notifyConnectionOn();
        } else {
            // Internet is not connected, retry after a delay
            currentRetryCount++;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::checkInternetConnection, getRetryDelay(currentRetryCount));
        }
    }

    private long getRetryDelay(int retryCount) {
        // Define your retry delay logic here, e.g., 30 seconds, 1 minute, etc.
        // You can adjust the delay based on the retryCount if needed.
        return 30_000; // 30 seconds in milliseconds
    }

    private void notifyConnectionOn() {
        for (InternetConnectionListener listener : listeners) {
            listener.onConnectionOn();
        }
    }

    private void notifyConnectOff() {
        for (InternetConnectionListener listener : listeners) {
            listener.onConnectOff();
        }
    }
}
