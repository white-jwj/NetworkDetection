package com.example.networkdetection.demo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class NetworkChange {
    ConnectivityManager connectivityManager;
    InternetConnectionStatusHelper connectionStatusHelper;
    private static NetworkChange instance;
    private final String TAG = "NetworkChange";
    private boolean isCellular;
    private boolean lastCellular;
    private boolean isProxy;
    private boolean lastProxy;
    private boolean lastWifi;
    WifiManager wifiManager;
    private final Context context;
    private NetworkChange(Context context) {
        this.context = context;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectionStatusHelper = InternetConnectionStatusHelper.getInstance(context);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        isCellular = getMobileDataState();
        lastCellular = isCellular;
        lastWifi = wifiManager.isWifiEnabled();
        isProxy = getProxy(context);
        lastProxy = getProxy(context);
        isProxy = false;
        Log.d(TAG, "NetworkChange: 初始化 各个状态:"+ this);
    }
    public static synchronized NetworkChange getInstance(Context context){
        if (instance==null){
            instance = new NetworkChange(context);
        }
        return instance;
    }
    public void startMonitor(){
        connectivityManager.registerNetworkCallback(networkRequest,networkCallback);
        if (!isCellular&&!wifiManager.isWifiEnabled()){
            connectionStatusHelper.setDisconnect();
        }
    }
    NetworkRequest networkRequest = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build();
    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            Log.d(TAG, "onCapabilitiesChanged: has changed");
            isCellular = getMobileDataState();
            isProxy = getProxy(context);
            if (isCellular!=lastCellular||isProxy!=lastProxy||wifiManager.isWifiEnabled()!=lastWifi){
                Log.d(TAG, "onCapabilitiesChanged: 数据源变化->检测");
                lastProxy = isProxy;
                lastCellular = isCellular;
                lastWifi = wifiManager.isWifiEnabled();
                connectionStatusHelper.checkConnection();
            }
            //系统检测连通直接使用，若没连通自行检测
            Log.d(TAG,networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)+"系统<--->上次检测 "+connectionStatusHelper.getCurrentInternetConnectionStatus());
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==connectionStatusHelper.getCurrentInternetConnectionStatus()){
                connectionStatusHelper.setConnect();
            }else {
                connectionStatusHelper.checkConnection();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            isCellular = getMobileDataState();
            isProxy = getProxy(context);
            //wifi和数据流量开关
            if (!isCellular&&!wifiManager.isWifiEnabled()){
                connectionStatusHelper.setDisconnect();
            }else if (isCellular!=lastCellular||isProxy!=lastProxy||wifiManager.isWifiEnabled()!=lastWifi){
                Log.d(TAG, "onCapabilitiesChanged: 数据源变化->检测");
                lastProxy = isProxy;
                lastCellular = isCellular;
                lastWifi = wifiManager.isWifiEnabled();
                connectionStatusHelper.checkConnection();
            }
            super.onLost(network);
        }
    };

    //获取手机数据开关状态
    public boolean getMobileDataState() {
        try {
            TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            if (getDataEnabled!=null) {
                return (Boolean) getDataEnabled.invoke(telephonyService);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    //获取代理
    public boolean getProxy(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyInfo proxyInfo = connectivityManager.getDefaultProxy();
        if (proxyInfo!=null){
            String host = proxyInfo.getHost();
            int port = proxyInfo.getPort();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            connectionStatusHelper.setmProxy(proxyInfo);
            return true;
        }else {
            return false;
        }
    }
    public void onDestroy(){
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    public String toString() {
        return "NetworkChange{" +
                "isCellular=" + isCellular +
                ", lastCellular=" + lastCellular +
                ", isProxy=" + isProxy +
                ", lastProxy=" + lastProxy +
                ", lastWifi=" + lastWifi +
                '}';
    }
}
