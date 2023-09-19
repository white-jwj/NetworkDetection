package com.example.networkdetection.demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.LayoutInflater;

import com.example.networkdetection.R;
import com.example.networkdetection.databinding.ActivityMainBinding;

import java.util.Map;

public class NetworkService extends Service {
    ActivityMainBinding mainBinding;
    InternetConnectionStatusHelper connectionStatusHelper;
    public NetworkService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainBinding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(this);
        connectionStatusHelper.addInternetConnectionListener(new InternetConnectionStatusHelper.InternetConnectionListener() {
            @Override
            public void onConnectionOn() {

            }
            @Override
            public void onConnectOff() {

            }

            @Override
            public void onChecking() {
                mainBinding.checkStatusTv.setText(R.string.checking);
            }

            @Override
            public void onChecked(Map<String,String> result) {
                mainBinding.checkStatusTv.setText(R.string.checked);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        connectionStatusHelper.checkInternetConnection();
        return null;
    }
}