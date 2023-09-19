package com.example.networkdetection.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import androidx.annotation.NonNull;


public class NetworkChange {
    ConnectivityManager connectivityManager;
    InternetConnectionStatusHelper connectionStatusHelper;
    private static NetworkChange instance;
    private final String TAG = "NetworkChange";

    private NetworkChange(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(context);
    }
    public static synchronized NetworkChange getInstance(Context context){
        if (instance==null){
            instance = new NetworkChange(context);
        }
        return instance;
    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            if(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    connectionStatusHelper.checkInternetConnection("蜂窝流量");
                    Log.d(TAG, "===当前在使用蜂窝流量上网===");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    connectionStatusHelper.checkInternetConnection("WIFI");
                    Log.d(TAG, "====当前在使用WiFi上网===");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                    connectionStatusHelper.checkInternetConnection("蓝牙");
                    Log.d(TAG, "=====当前使用蓝牙上网=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                    Log.d(TAG, "=====当前使用以太网上网=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    connectionStatusHelper.checkInternetConnection("VPN");
                    Log.d(TAG, "===当前使用VPN上网====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                    Log.d(TAG, "===表示此网络使用Wi-Fi感知传输====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
                    Log.d(TAG, "=====表示此网络使用LoWPAN传输=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) {
                    Log.d(TAG, "=====表示此网络使用USB传输=====");
                }
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            connectionStatusHelper.checkInternetConnection("网络已断开");
            Log.d(TAG, "=====表示此网络已断开=====");
        }
    };
}
