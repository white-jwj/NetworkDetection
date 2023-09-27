package com.example.networkdetection.demo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternetConnectionStatusHelper {
    private final String TAG = "InternetConnectionStatusHelper";
    private static InternetConnectionStatusHelper instance;
    private final Context context;
    private final List<InternetConnectionListener> listeners;
    private boolean isChecking;
    private boolean isWaiting;
    private static AtomicBoolean result;
    private int currentRetryCount;
    private final Map<String,String> resultMap; //Debug
    private String checkStage = "NULL";
    private final String stage1 = "30s";
    private final String stage2 = "1min";
    private final String stage3 = "30min";
    private long maxWait = 1_800_000L;
    private final static String TYPE = "NET_TYPE";
    private volatile ProxyInfo mProxy;
    private String FLAG = "NULL";
    ScheduledExecutorService netWorkScheduledExecutorService;
    private InternetConnectionStatusHelper(Context context) {
        mProxy = null;
        result = new AtomicBoolean();
        result.set(false);
        isChecking = false;
        isWaiting = false;
        currentRetryCount = 0;
        this.context = context.getApplicationContext();
        listeners = new ArrayList<>();
        resultMap = new HashMap<>();
        netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }
    public static synchronized InternetConnectionStatusHelper getInstance(Context context) {
        if (instance == null) {
            instance = new InternetConnectionStatusHelper(context);
        }
        return instance;
    }
    //DNS
    private  boolean isDnsServerReachable(String host) {
        Log.d(TAG, "isDnsServerReachable: "+host+":53");
        try {
            Socket socket = new Socket();// 端口 ; 3 seconds timeout
            socket.connect(new InetSocketAddress(host, 53), 3000); // 端口 ; 3 seconds timeout
            socket.close();
            Log.d(TAG, "isDnsServerReachable: TRUE");
            return true;
        } catch (IOException | IllegalArgumentException e ) {
            Log.d(TAG, "isDnsServerReachable: FALSE "+e.getMessage());
            return false;
        }
    }


    //Web
    private  boolean isWebServerReachable(String url) {
        Log.d(TAG, "isWebServerReachable: "+url+":443");
        try {
            if (mProxy!=null){
                Log.d(TAG, "isWebServerReachable: "+mProxy.toString());
                Socket socket = new Socket(mProxy.getHost(),mProxy.getPort());
                Log.d(TAG, "isWebServerReachable isConnected: "+socket.isConnected());
                return socket.isConnected();
            }else {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(url,443),3000);
                Log.d(TAG, "isWebServerReachable: else "+socket.isConnected());
                socket.close();
                return true;
            }
        } catch (IllegalArgumentException | IOException e) {
            Log.d(TAG, "isWebServerReachable: FALSE "+e.getMessage());
            return false;
        }
    }
    private synchronized boolean checkingConnection() throws InterruptedException {
        Log.d(TAG, "检测中 = "+FLAG+" 当前线程： "+Thread.currentThread().getName());
        isChecking = true;
        isWaiting = false;
        notifyChecking();
        // DNS服务器列表
        List<String> dnsServers = new ArrayList<>();
        dnsServers.add("8.8.8.8");
        dnsServers.add("114.114.114.114");
        // Web服务器列表
        List<String> webServers = new ArrayList<>();
        webServers.add("google.com");
        webServers.add("baidu.com");

        for (String dnsServer : dnsServers) {
            if (isDnsServerReachable(dnsServer)) {
                // 任意一个DNS服务器可达，继续Web服务器检测
                for (String webServer : webServers) {
                    if (isWebServerReachable(webServer)) {
                        // 任意一个Web服务器可达，互联网处于连通状态
                        result.set(true);
                        return true;
                    }
                }
            }
        }
        result.set(false);
        return false;
    }
    //用户手动触发
    public void triggerInternetConnectionStatusCheck() {
        if (isChecking) {
            // 正在检测中 继续 返回最终结果
            waitResult();
        }else {
            new Thread(()->{
                // 非检测中 判断处于哪个阶段  30min的话重新开始 初始化
                switch (checkStage){
                    case stage1:
                    case stage2:
                        Log.d(TAG, "跳过等待 直接开始"+checkStage);
                        FLAG = "stage1|2";
                        checkInternetConnection();
                        break;
                    case stage3:
                        Log.d(TAG, "在最后一个空闲时间 重置条件 跳过等待 开始新一轮检测");
                        currentRetryCount = 0;
                        FLAG = "stage3";
                        checkInternetConnection();
                        break;
                    default:
                        break;
                }
        }).start();
        }
    }
    public void checkConnection(String flag){
        FLAG = flag;
        if (isChecking){
            netWorkScheduledExecutorService.shutdown();
            waitResult();
        }else {
            if (netWorkScheduledExecutorService.isShutdown()){
                netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
            //new Thread(this::checkInternetConnection).start();
            netWorkScheduledExecutorService.schedule(this::checkInternetConnection,0,TimeUnit.SECONDS);
        }
    }
    //检测结果
    private synchronized void checkInternetConnection() {
        setProxy();
        try{
            if (checkingConnection()) {
                notifyConnectionOn();
                // 网络连通 回调通知
                isChecking = false;
                isWaiting = true;
                checkStage = stage3;
                currentRetryCount = 0;
                FLAG = "Connect success";
                Log.d(TAG, "checkInternetConnection: 连通 等待时间: "+getRetryDelay(3)/1000);
                if (netWorkScheduledExecutorService.isShutdown()){
                    netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
                netWorkScheduledExecutorService.schedule(this::checkInternetConnection, getRetryDelay(3), TimeUnit.MILLISECONDS);
            } else {
                // 网络未连通，延迟重新检测
                Log.d(TAG, "isWaiting next: "+netWorkScheduledExecutorService.toString());
                isChecking = false;
                isWaiting = true;
                currentRetryCount++;
                //子线程耗时操作  ()
                long retryDelay = getRetryDelay(currentRetryCount);
                notifyWaiting(retryDelay);
                FLAG = "delay "+(retryDelay/1000);
                if (netWorkScheduledExecutorService.isShutdown()){
                    netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
                ScheduledFuture<?> schedule = netWorkScheduledExecutorService.schedule(this::checkInternetConnection, retryDelay, TimeUnit.MILLISECONDS);

                Log.d(TAG, "isWaiting next: "+schedule);
            }
            if (currentRetryCount >= 3) {
                // 达到最大重测次数 重置并回调通知
                mProxy = null;
                isChecking = false;
                isWaiting = true;
                long retryDelay = getRetryDelay(currentRetryCount);
                currentRetryCount = 0;
                //等30分钟 重复
                FLAG = "Connect false";
                Log.d(TAG, "checkInternetConnection: 没网");
                notifyWaiting(retryDelay);
                notifyConnectOff();
            }
        }catch (InterruptedException e){
            Log.d(TAG, "triggerInternetConnectionStatusCheck: "+e.getMessage());
        }
    }
    public void setConnect(){
        resultMap.put(TYPE,"连接");
        //notifyChecked(resultMap);
        isChecking = false;
        currentRetryCount = 0;
        netWorkScheduledExecutorService.schedule(this::checkInternetConnection,getRetryDelay(3), TimeUnit.MILLISECONDS);
        notifyWaiting(getRetryDelay(3));
        notifyConnectionOn();
    }
    public void setDisconnect(){
        if (netWorkScheduledExecutorService.isShutdown()){
            netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
        notifyWaiting(getRetryDelay(3));
        notifyConnectOff();
        resultMap.put(TYPE,"无连接");
        result.set(false);
        //notifyChecked(resultMap);
        isChecking = false;
        currentRetryCount = 0;
        FLAG = "setDisconnect";
        Log.d(TAG, "setDisconnect: "+netWorkScheduledExecutorService.toString());
        netWorkScheduledExecutorService.schedule(this::checkInternetConnection,getRetryDelay(3), TimeUnit.MILLISECONDS);
    }
    public void setmProxy(ProxyInfo proxy){
        mProxy = proxy;
    }
    private void setProxy(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyInfo proxyInfo = connectivityManager.getDefaultProxy();
        mProxy = proxyInfo;
    }

    private long getRetryDelay(int retryCount) {
        long[] retryDelays = {30_000L, 60_000L, maxWait}; // 30 seconds, 1 minute, 30 minutes
        String[] stages = {stage1,stage2,stage3};
        // 根据重试次数选择对应的重试延迟
        if (retryCount >= 1 && retryCount <= retryDelays.length) {
            checkStage = stages[retryCount-1];
            return retryDelays[retryCount - 1];
        }
        // 如果超出了固定值的范围，返回默认的最大延迟（30分钟）
        checkStage = stages[stages.length - 1];
        return retryDelays[retryDelays.length - 1];
    }
    private void waitResult(){
        Log.d(TAG, "wait result");
    }
    //检测结果
    public boolean getCurrentInternetConnectionStatus(){
        return result.get();
    }
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }
    public interface InternetConnectionListener {
        void onConnectionOn();//连通
        void onConnectOff();//未连通
        void onChecking();//Debug
        //*void onChecked(Map<String,String> result);//Debug
        void onWaiting(long time);//Debug
    }
    public void addInternetConnectionListener(InternetConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    public void removeInternetConnectionListener(InternetConnectionListener listener) {
        listeners.remove(listener);
    }
    private void notifyConnectionOn() {
        for (InternetConnectionListener listener : listeners) {
            listener.onConnectionOn();
        }
    }
    private void notifyConnectOff() {
        for (InternetConnectionListener listener : listeners) {
            listener.onConnectOff();
        }
    }
    private void notifyChecking(){
        for (InternetConnectionListener listener : listeners) {
            listener.onChecking();
        }
    }
    /*private void notifyChecked(Map<String,String> result){
        for (InternetConnectionListener listener : listeners) {
            listener.onChecked(result);
        }
    }*/
    private void notifyWaiting(long time){
        for (InternetConnectionListener listener : listeners) {
            listener.onWaiting(time);
        }
    }
}
