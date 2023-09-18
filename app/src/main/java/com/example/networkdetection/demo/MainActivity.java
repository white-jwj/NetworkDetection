package com.example.networkdetection.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.networkdetection.R;
import com.example.networkdetection.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding mainBinding;
    InternetConnectionStatusHelper internetConnectionHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        internetConnectionHelper = InternetConnectionStatusHelper.getInstance(this);
        //手动触发检测
        mainBinding.netDetectionBt.setOnClickListener(view -> internetConnectionHelper.triggerInternetConnectionStatusCheck());
        internetConnectionHelper.addInternetConnectionListener(new InternetConnectionStatusHelper.InternetConnectionListener() {
            @Override
            public void onConnectionOn() {
                runOnUiThread(() -> changeConnectionIm(true));
            }
            @Override
            public void onConnectOff() {
                runOnUiThread(()->changeConnectionIm(false));
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