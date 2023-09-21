package com.example.networkdetection.demo;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class NetworkService extends Service {
    private final String TAG = "NetworkService";
    InternetConnectionStatusHelper connectionStatusHelper;
    NetworkChange networkChange;
    NetworkReceiver networkReceiver;
    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.conn.PROXY_CHANGE");
        networkReceiver = new NetworkReceiver();
       // registerReceiver(networkReceiver,intentFilter);
        Log.d(TAG, "服务创建了");
        super.onCreate();
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(this);
        networkChange = NetworkChange.getInstance(this);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动了: ");
        networkChange.startMonitor();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onDestroy() {
        networkChange.onDestroy();
        unregisterReceiver(networkReceiver);
        super.onDestroy();
    }

}