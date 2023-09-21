package com.example.networkdetection.demo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import com.example.networkdetection.R;
import com.example.networkdetection.databinding.ActivityMainBinding;
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
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        NetworkReceiver networkReceiver = new NetworkReceiver();
        registerReceiver(networkReceiver,intentFilter);
        IntentFilter intentFilter1 = new IntentFilter("android.intent.action.PROXY_CHANGE");
        TestReceiver testReceiver = new TestReceiver();
        registerReceiver(testReceiver,intentFilter1);
        //手动触发检测
        mainBinding.netDetectionBt.setOnClickListener(view -> {
            Log.d("InternetConnectionStatusHelper", "手动触发");
            mainBinding.checkStatusTv.setText(R.string.checking);
            //internetConnectionHelper.triggerInternetConnectionStatusCheck();
        });
        mainBinding.netServiceBt.setOnClickListener(view -> {
            mainBinding.checkStatusTv.setText("服务启动");
            //startService(new Intent(this,NetworkService.class));
        });
        internetConnectionHelper.addInternetConnectionListener(new InternetConnectionStatusHelper.InternetConnectionListener() {
            @Override
            public void onConnectionOn() {
                runOnUiThread(() -> changeConnectionIm(true));
            }
            @Override
            public void onConnectOff() {
                runOnUiThread(()->changeConnectionIm(false));
            }

            @Override
            public void onChecking() {
                runOnUiThread(()->{
                    mainBinding.checkStatusTv.setText(R.string.checking);
                });
            }
            @Override
            public void onChecked(Map<String,String> result) {
                runOnUiThread(()->mainBinding.checkStatusTv.setText(R.string.checked));
                String s = result.get("NET_TYPE");
                if (s!=null){
                    runOnUiThread(()->mainBinding.netTypeResultTv.setText(s));
                }
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