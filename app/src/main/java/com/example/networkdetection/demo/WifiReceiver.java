package com.example.networkdetection.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import java.util.Arrays;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NetworkChange", "WifiReceiver 代理接收器接收到了: "+ intent.getAction());
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d("NetworkChange", "onReceive: "+ Arrays.toString(connectivityManager.getAllNetworkInfo()));
    }
}
