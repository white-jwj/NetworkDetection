package com.example.networkdetection.demo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.util.Log;
import androidx.annotation.NonNull;

public class NetworkChange {
    ConnectivityManager connectivityManager;
    InternetConnectionStatusHelper connectionStatusHelper;
    private static NetworkChange instance;
    private final String TAG = "NetworkChange";
    private boolean wifiEnable;
    private boolean lastConnectStatus;
    private boolean isCellular;
    private boolean hasOpen;
    WifiManager wifiManager;
    private NetworkChange(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(context);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiEnable = wifiManager.isWifiEnabled();
        isCellular = true;
        hasOpen = true;
    }
    public static synchronized NetworkChange getInstance(Context context){
        if (instance==null){
            instance = new NetworkChange(context);
        }
        return instance;
    }
    public void startMonitor(){
        connectivityManager.registerNetworkCallback(networkRequest,networkCallback);
    }
    NetworkRequest networkRequest = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build();
    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            Log.d(TAG, "onCapabilitiesChanged: has changed");
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                isCellular = true;
            }
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiEnable = true;
            }
            Log.d(TAG, "network: "+network.getNetworkHandle()+" networkCapabilities: "+networkCapabilities);
            //系统检测连通直接使用，若没连通自行检测
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)){
                connectionStatusHelper.setConnect();
                Log.d(TAG,networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)+"系统<--->上次检测 "+lastConnectStatus);
            }else {
                connectionStatusHelper.checkInternetConnection();
            }

            lastConnectStatus = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            //wifi和数据流量开关
            if (!isCellular&&!wifiEnable){
                connectionStatusHelper.setDisconnect();
                hasOpen = false;
            }
            if ((isCellular||wifiEnable)&&!hasOpen){
                hasOpen = true;
                connectionStatusHelper.checkInternetConnection();
            }

        }
    };


    public void onDestroy(){
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }


}
