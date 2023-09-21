package com.example.networkdetection.demo;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternetConnectionStatusHelper {
    private final String TAG = "InternetConnectionStatusHelper";
    private static InternetConnectionStatusHelper instance;
    private final Context context;
    private final List<InternetConnectionListener> listeners;
    private boolean isChecking;
    private static AtomicBoolean result;
    private int currentRetryCount;
    private final Map<String,String> resultMap; //Debug
    private String checkStage = "NULL";
    private final String stage1 = "30s";
    private final String stage2 = "1min";
    private final String stage3 = "30min";
    private long maxWait = 1800;
    private final static String TYPE = "NET_TYPE";
    private final static String CONNECT_RESULT ="CONNECT_RESULT";
    ScheduledExecutorService netWorkScheduledExecutorService;
    private InternetConnectionStatusHelper(Context context) {
        result = new AtomicBoolean();
        currentRetryCount = 0;
        this.context = context.getApplicationContext();
        listeners = new ArrayList<>();
        resultMap = new HashMap<>();
        netWorkScheduledExecutorService = Executors.newScheduledThreadPool(3);
    }
    public static synchronized InternetConnectionStatusHelper getInstance(Context context) {
        if (instance == null) {
            instance = new InternetConnectionStatusHelper(context);
        }
        return instance;
    }
    //DNS
    private boolean isDnsServerReachable(String host) {
        Log.d(TAG, "isDnsServerReachable: "+host+":53");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, 53), 3000); // 端口 ; 3 seconds timeout
            socket.close();
            Log.d(TAG, "isDnsServerReachable: TRUE");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "isDnsServerReachable: FALSE");
            return false;
        }
    }
    //Web
    private boolean isWebServerReachable(String url) {
        Log.d(TAG, "isWebServerReachable: "+url+":443");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(url, 443), 3000); // 端口 ; 3 seconds timeout
            socket.close();
            Log.d(TAG, "isWebServerReachable: TRUE");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "isWebServerReachable: FALSE");
            return false;
        }
    }
    private boolean checkingConnection() throws InterruptedException {
        Log.d(TAG, "检测中");
        isChecking = true;
        notifyChecking();
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
        resultMap.put(CONNECT_RESULT, String.valueOf(result.get()));
        notifyChecked(resultMap);
        return result.get();
    }
    //用户手动触发
    public void triggerInternetConnectionStatusCheck() {
        if (isChecking) {
            // 正在检测中 继续 返回最终结果
            Log.d(TAG, "等结果");
        }else {
            netWorkScheduledExecutorService.shutdownNow();
            // 非检测中 判断处于哪个阶段  30min的话重新开始 初始化
            switch (checkStage){
                case stage1:
                case stage2:
                    Log.d(TAG, "跳过等待 直接开始"+checkStage);
                    checkInternetConnection();
                    break;
                case stage3:
                    Log.d(TAG, "在最后一个空闲时间 重置条件 跳过等待 开始新一轮检测");
                    currentRetryCount = 0;
                    checkInternetConnection();
                    break;
                default:
                    break;
            }
        }
    }
    //检测结果
    public void checkInternetConnection() {
        try{
            if (checkingConnection()) {
                // 网络连通 回调通知
                isChecking = false;
                checkStage = stage3;
                currentRetryCount = 0;
                Log.d(TAG, "checkInternetConnection: 连通");
                netWorkScheduledExecutorService.schedule(()->checkInternetConnection(), getRetryDelay(3), TimeUnit.SECONDS);
                notifyWaiting(getRetryDelay(3));
                notifyConnectionOn();
            } else {
                // 网络未连通，延迟重新检测
                isChecking = false;
                currentRetryCount++;
                //子线程耗时操作  ()
                long retryDelay = getRetryDelay(currentRetryCount);
                netWorkScheduledExecutorService.schedule(()->checkInternetConnection(), retryDelay, TimeUnit.SECONDS);
                notifyWaiting(retryDelay);
                Log.d(TAG, "checkInternetConnection: 重测第 "+currentRetryCount+" 次");
            }
            if (currentRetryCount >= 3) {
                // 达到最大重测次数 重置并回调通知
                isChecking = false;
                long retryDelay = getRetryDelay(currentRetryCount);
                currentRetryCount = 0;
                //等30分钟 重复
                netWorkScheduledExecutorService.schedule(()->checkInternetConnection(), retryDelay, TimeUnit.SECONDS);
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
        notifyChecked(resultMap);
        isChecking = false;
        currentRetryCount = 0;
        netWorkScheduledExecutorService.schedule(()->checkInternetConnection(),getRetryDelay(3), TimeUnit.SECONDS);
        notifyWaiting(getRetryDelay(3));
        notifyConnectionOn();
    }
    public void setDisconnect(){
        resultMap.put(TYPE,"无连接");
        notifyChecked(resultMap);
        isChecking = false;
        currentRetryCount = 0;
        netWorkScheduledExecutorService.schedule(()->checkInternetConnection(),getRetryDelay(3), TimeUnit.SECONDS);
        notifyWaiting(getRetryDelay(3));
        notifyConnectOff();
    }

    public void checkInternetConnection(String netType) {
        resultMap.put(TYPE,netType);
        checkInternetConnection();
    }
    private long getRetryDelay(int retryCount) {
        long[] retryDelays = {30, 60, maxWait}; // 30 seconds, 1 minute, 30 minutes
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
        void onChecked(Map<String,String> result);//Debug
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
    private void notifyChecked(Map<String,String> result){
        for (InternetConnectionListener listener : listeners) {
            listener.onChecked(result);
        }
    }
    private void notifyWaiting(long time){
        for (InternetConnectionListener listener : listeners) {
            listener.onWaiting(time);
        }
    }
}
