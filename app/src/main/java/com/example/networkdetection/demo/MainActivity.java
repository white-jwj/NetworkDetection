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

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivityNetWork";
    ActivityMainBinding mainBinding;
    InternetConnectionStatusHelper internetConnectionHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        internetConnectionHelper = InternetConnectionStatusHelper.getInstance(this);
        //手动触发检测
        mainBinding.netDetectionBt.setOnClickListener(view -> {
            Log.d("InternetConnectionStatusHelper", "手动触发");
            internetConnectionHelper.triggerInternetConnectionStatusCheck();
        });
        //启动服务
        mainBinding.netServiceBt.setOnClickListener(view -> {
            mainBinding.checkStatusTv.setText("服务启动");
            startService(new Intent(this,NetworkService.class));
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
                runOnUiThread(()->mainBinding.checkStatusTv.setText(String.valueOf(time)));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}