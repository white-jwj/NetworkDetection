package com.example.networkdetection.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.example.networkdetection.R;
import com.example.networkdetection.databinding.ActivityMainBinding;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivityNetWork";
    ActivityMainBinding mainBinding;
    InternetConnectionStatusHelper internetConnectionHelper;
    private Handler handler;
    private long time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        handler = new Handler(getMainLooper());
        //demo
        internetConnectionHelper = InternetConnectionStatusHelper.getInstance(this);
        //启动服务
        mainBinding.netServiceBt.setOnClickListener(view -> {
            mainBinding.checkStatusTv.setText("服务启动");
           //startService(new Intent(this,NetworkService.class));
            /* debug
             * 前台服务
             **//*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this,MyForegroundService.class));
            }*/
            //debug
            WifiReceiver wifiReceiver = new WifiReceiver();
            IntentFilter intentFilter1 = new IntentFilter();
            intentFilter1.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter1.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intentFilter1.addAction(WifiManager.RSSI_CHANGED_ACTION);
            intentFilter1.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            intentFilter1.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            registerReceiver(wifiReceiver, intentFilter1);
            NetworkChange networkChange = NetworkChange.getInstance(this);
            networkChange.startMonitor();
        });
        //手动触发检测
        mainBinding.netDetectionBt.setOnClickListener(view -> {
            Log.d("InternetConnectionStatusHelper", "手动触发");
            internetConnectionHelper.triggerInternetConnectionStatusCheck();
        });
        internetConnectionHelper.addInternetConnectionListener(new InternetConnectionStatusHelper.InternetConnectionListener() {
            @Override
            public void onConnectionOn() {
                runOnUiThread(() -> {
                    changeConnectionIm(true);
                    mainBinding.netTypeResultTv.setText("连接");
                });
            }
            @Override
            public void onConnectOff() {
                runOnUiThread(() -> {
                    changeConnectionIm(false);
                    mainBinding.netTypeResultTv.setText("未连接");
                });
            }

            @Override
            public void onChecking() {
                runOnUiThread(()->{
                    mainBinding.checkStatusTv.setText(R.string.checking);
                });
            }
            @Override
            public void onChecked(Map<String,String> result) {

            }
            @Override
            public void onWaiting(long time) {
                runOnUiThread(()->mainBinding.checkStatusTv.setText(String.valueOf(time/1000)));
            }
        });
    }

    public void changeConnectionIm(boolean connection){
        if (connection){
            mainBinding.netConnectionIv.setImageResource(R.drawable.connected);
        }else {
            mainBinding.netConnectionIv.setImageResource(R.drawable.disconnect);
        }
    }
}