package com.example.networkdetection.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ProxyInfo;
import android.util.Log;

public class NetworkReceiver extends BroadcastReceiver {
    int i = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NetworkChange", "onReceiveTest  wifi Change: 接收到了"+i++ + intent.getAction());
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyInfo proxyInfo = connectivityManager.getDefaultProxy();
        if (proxyInfo != null) {
            Log.d("NetworkChange", "onReceive has: 代理");
        }

    }
}
