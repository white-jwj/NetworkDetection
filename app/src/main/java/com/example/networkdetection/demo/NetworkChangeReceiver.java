package com.example.networkdetection.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
    InternetConnectionStatusHelper connectionStatusHelper;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.d("TAG", "onReceive: my receiver receive net has change");
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(context);
        connectionStatusHelper.triggerInternetConnectionStatusCheck();
    }
}