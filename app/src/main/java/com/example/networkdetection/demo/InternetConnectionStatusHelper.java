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
    private String TAG = "InternetConnectionStatusHelper";
    private static InternetConnectionStatusHelper instance;
    private final Context context;
    private final List<InternetConnectionListener> listeners;
    private boolean isChecking;
    private boolean isWaiting;
    private static AtomicBoolean result;
    private int currentRetryCount;
    private Map<String,String> resultMap;
    private final static String TYPE = "NET_TYPE";
    private final static String CONNECT_RESULT ="CONNECT_RESULT";
    private InternetConnectionStatusHelper(Context context) {
        result = new AtomicBoolean();
        currentRetryCount = 0;
        this.context = context.getApplicationContext();
        listeners = new ArrayList<>();
        resultMap = new HashMap<>();
    }

    public static synchronized InternetConnectionStatusHelper getInstance(Context context) {
        if (instance == null) {
            instance = new InternetConnectionStatusHelper(context);
        }
        return instance;
    }
    //DNS
    private boolean isDnsServerReachable(String host) {
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

    private boolean checkConnection() throws InterruptedException {
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
        // 所有DNS服务器和Web服务器均不可达，互联网不可用
        notifyChecked(resultMap);
        return result.get();
    }

    //用户手动触发
    public void triggerInternetConnectionStatusCheck() {
        if (isChecking) {

            // 正在检测中 继续 返回最终结果
            isWaiting = false;
            return;
        }
        //非检测中 重新开始 初始化
        isChecking = true;
        currentRetryCount = 0; //
        checkInternetConnection();
    }

    public void checkInternetConnection() {
        try{
            if (currentRetryCount >= 3) {
                // 达到最大重测次数 重置并回调通知
                isChecking = false;
                isWaiting = true;
                long retryDelay = getRetryDelay(currentRetryCount);
                currentRetryCount = 0;
                //等30分钟 重复
                ScheduledExecutorService netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                netWorkScheduledExecutorService.schedule(()->checkInternetConnection(), retryDelay, TimeUnit.SECONDS);
                Log.d(TAG, "checkInternetConnection: 没网");
                notifyConnectOff();
                return;
            }

            if (checkConnection()) {
                // 网络连通 回调通知
                isChecking = false;
                isWaiting = true;
                currentRetryCount = 0;
                Log.d(TAG, "checkInternetConnection: 联通");
                notifyConnectionOn();
            } else {
                // 网络未连通，延迟重新检测
                isChecking = false;
                isWaiting = true;
                currentRetryCount++;
                //子线程耗时操作  ()
                ScheduledExecutorService netWorkScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                netWorkScheduledExecutorService.schedule(()->checkInternetConnection(),getRetryDelay(currentRetryCount), TimeUnit.SECONDS);
                Log.d(TAG, "checkInternetConnection: 重测第 "+currentRetryCount+" 次");
            }
        }catch (InterruptedException e){
            Log.d(TAG, "triggerInternetConnectionStatusCheck: "+e.getMessage());
        }

    }
    public void checkInternetConnection(String netType) {
        resultMap.put(TYPE,netType);
        checkInternetConnection();
    }
    private long getRetryDelay(int retryCount) {
        long[] retryDelays = {30_000, 60_000, 1_800_000}; // 30 seconds, 1 minute, 30 minutes
        // 根据重试次数选择对应的重试延迟
        if (retryCount >= 1 && retryCount <= retryDelays.length) {
            return retryDelays[retryCount - 1];
        }
        // 如果超出了固定值的范围，返回默认的最大延迟（30分钟）
        return retryDelays[retryDelays.length - 1];
    }

    public interface InternetConnectionListener {
        void onConnectionOn();//连通
        void onConnectOff();//未连通
        void onChecking();
        void onChecked(Map<String,String> result);
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
}
