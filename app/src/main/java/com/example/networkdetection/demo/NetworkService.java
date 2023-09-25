package com.example.networkdetection.demo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class NetworkService extends Service {
    private final String TAG = "NetworkChange";
    InternetConnectionStatusHelper connectionStatusHelper;
    NetworkChange networkChange;
    BroadcastReceiver networkReceiver;
    private PowerManager.WakeLock wakeLock;
    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PROXY_CHANGE");
        intentFilter.addAction("android.intent.action.PROXY_CHANGE");
        networkReceiver = new NetworkReceiver();
        registerReceiver(networkReceiver,intentFilter);
        //wakelock 保持存活
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "networkdetection:NetWakeLock");
        wakeLock.acquire();
        Log.d(TAG, "服务创建了");
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(this);
        networkChange = NetworkChange.getInstance(this);
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动了: ");
        networkChange.startMonitor();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new Binder();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "my service onDestroy: ");
        networkChange.onDestroy();
        wakeLock.release();
        unregisterReceiver(networkReceiver);
        super.onDestroy();
    }

}