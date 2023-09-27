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
    private final ConnectivityManager connectivityManager;
    private final InternetConnectionStatusHelper connectionStatusHelper;
    private static NetworkChange instance;
    private final String TAG = "NetworkChange";
    private boolean isFirst;
    private boolean isCellular;
    private boolean lastCellular;
    private boolean isProxy;
    private boolean lastProxy;
    private boolean lastWifi;
    private boolean lastVpn;
    private boolean isVpn;
    private final String VPN = "VPN";
    private final WifiManager wifiManager;
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
        isVpn = false;
        lastVpn = false;
        isFirst = true;
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
        connectivityManager.addDefaultNetworkActiveListener(() -> {
            Log.d(TAG, "onCapabilitiesChanged isDefaultNetworkActive: "+connectivityManager.isDefaultNetworkActive());
        });
    }
    NetworkRequest networkRequest = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();
    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "onCapabilitiesChanged:Network is available");
            super.onAvailable(network);
        }
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            Log.d(TAG, "onCapabilitiesChanged: getNetworkInfo: "+connectivityManager.getNetworkInfo(network));
            if (isFirst){
                connectionStatusHelper.checkConnection("第一次");
                isFirst = false;
            }else {
                checkDetection(network);
                //系统网络参数,检测连通,若没连通自行检测
                if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    Log.d(TAG, "onCapabilitiesChanged 2: 数据源变化->检测");
                    connectionStatusHelper.checkConnection("数据源变化 2");
                }
            }
        }
        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "onLost: 数据源变化->检测");
            isCellular = getMobileDataState();
            isProxy = getProxy(context);
            if (connectivityManager.getNetworkInfo(network)!=null){
                isVpn = connectivityManager.getNetworkInfo(network).getTypeName().equals(VPN);
            }else {
                isVpn = false;
            }
            //wifi和数据流量开关
            if (!isCellular&&!wifiManager.isWifiEnabled()){
                lastCellular = isCellular;
                connectionStatusHelper.setDisconnect();
            }else{
                checkDetection(network);
            }
            super.onLost(network);
        }
    };
    //系统网络参数变化 若数据、wifi、proxy开关没有改变则不进行检测
    public void checkDetection(Network network){
        isCellular = getMobileDataState();
        isProxy = getProxy(context);
        if (connectivityManager.getNetworkInfo(network)!=null){
            isVpn = connectivityManager.getNetworkInfo(network).getTypeName().equals(VPN);
        }else {
            isVpn = false;
        }
        if (isCellular!=lastCellular||isProxy!=lastProxy||wifiManager.isWifiEnabled()||isVpn!=lastVpn){
            Log.d(TAG, "onCapabilitiesChanged : 数据源变化->检测 "+ NetworkChange.this);
            lastProxy = isProxy;
            lastCellular = isCellular;
            lastWifi = wifiManager.isWifiEnabled();
            lastVpn = isVpn;
            connectionStatusHelper.checkConnection("数据源变化 ");
        }
    }

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
                ", isWifi=" + wifiManager.isWifiEnabled()+
                ", isVpn=" + isVpn+
                ", lastVpn=" + lastVpn+
                '}';
    }
}
