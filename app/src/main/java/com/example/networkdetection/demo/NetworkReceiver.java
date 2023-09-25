package com.example.networkdetection.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ProxyInfo;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class NetworkReceiver extends BroadcastReceiver {
    private PowerManager.WakeLock wakeLock;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NetworkChange", "onReceiveTest 代理接收器接收到了: "+ intent.getAction());
        //wakelock 保持存活
        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "networkdetection:NetWakeLock");
        wakeLock.acquire();

        InternetConnectionStatusHelper internetConnectionStatusHelper = InternetConnectionStatusHelper.getInstance(context);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyInfo proxyInfo = connectivityManager.getDefaultProxy();
        if (proxyInfo != null) {
            String host = proxyInfo.getHost();
            int port = proxyInfo.getPort();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            internetConnectionStatusHelper.setmProxy(proxyInfo);
            internetConnectionStatusHelper.checkConnection();
            Log.d("NetworkChange", "onReceive has: 代理");
        }else {
            internetConnectionStatusHelper.checkConnection();
            Log.d("NetworkChange", "onReceive: 无代理");
        }
    }

}
