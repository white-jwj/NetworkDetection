package com.example.networkdetection.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DnsEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;

import com.example.networkdetection.R;
import com.example.networkdetection.databinding.ActivityMainBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivityNetWork";
    ActivityMainBinding mainBinding;
    AtomicBoolean result;
    InternetConnectionStatusHelper internetConnectionHelper;
    ConnectivityManager cm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        result = new AtomicBoolean();
        NetworkChange networkChange = NetworkChange.getInstance(this);


        internetConnectionHelper = InternetConnectionStatusHelper.getInstance(this);
        //手动触发检测
        mainBinding.netDetectionBt.setOnClickListener(view -> {
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
                if (Boolean.parseBoolean(result.get("CONNECT_RESULT"))){
                    runOnUiThread(() -> changeConnectionIm(true));
                }else {
                    runOnUiThread(()->changeConnectionIm(false));
                }

            }
        });

    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            if(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.d(TAG, "===当前在使用Mobile流量上网===");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    runOnUiThread(()->mainBinding.netTypeResultTv.setText(R.string.net_type_WIFI));
                    Log.d(TAG, "====当前在使用WiFi上网===");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                    Log.d(TAG, "=====当前使用蓝牙上网=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.d(TAG, "=====当前使用以太网上网=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    Log.d(TAG, "===当前使用VPN上网====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                    Log.d(TAG, "===表示此网络使用Wi-Fi感知传输====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
                    Log.d(TAG, "=====表示此网络使用LoWPAN传输=====");
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) {
                    Log.d(TAG, "=====表示此网络使用USB传输=====");
                }else {
                    Log.d(TAG, "====无网络====");
                }
            }
        }
    };


    private boolean isDnsServerReachable(String host) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, 53), 3000); // 端口 ; 3 seconds timeout
            socket.close();
            Log.d(TAG, "isDnsServerReachable: TRUE");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "isDnsServerReachable: FALSE"+e.getMessage());
            return false;
        }
    }
    //Web
    private boolean isWebServerReachable(String url) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(url, 443), 3000); // 端口 ; 3 seconds timeout
            socket.close();
            Log.d(TAG, "isWebServerReachable: TRUE");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "isWebServerReachable: FALSE"+e.getMessage());
            return false;
        }
    }

    private boolean checkConnection() throws InterruptedException {
        // DNS服务器列表
        List<String> dnsServers = new ArrayList<>();
        dnsServers.add("8.8.8.8");
        dnsServers.add("114.114.114.114");

        // Web服务器列表
        List<String> webServers = new ArrayList<>();
        webServers.add("google.com");
        webServers.add("baidu.com");
        // 检测DNS服务器连通性
        Thread thread = new Thread(()->{
            for (String dnsServer : dnsServers) {
                if (isDnsServerReachable(dnsServer)) {
                    // 任意一个DNS服务器可达，继续Web服务器检测
                    for (String webServer : webServers) {
                        if (isWebServerReachable(webServer)) {
                            // 任意一个Web服务器可达，互联网处于连通状态
                            result.set(true);
                            break;
                        }else {
                            result.set(false);
                        }

                    }
                }else {
                    result.set(false);
                }
            }
        });
        thread.start();
        thread.join();
        // 所有DNS服务器和Web服务器均不可达，互联网不可用
       return result.get();
    }


    public void changeConnectionIm(boolean connection){
        if (connection){
            mainBinding.netConnectionIv.setImageResource(R.drawable.connected);
        }else {
            mainBinding.netConnectionIv.setImageResource(R.drawable.disconnect);
        }
    }
}