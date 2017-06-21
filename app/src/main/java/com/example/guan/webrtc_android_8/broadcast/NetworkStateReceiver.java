package com.example.guan.webrtc_android_8.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.guan.webrtc_android_8.common.AppRTC_Common;
import com.example.guan.webrtc_android_8.utils.ShowUtil;

/**
 * Created by guan on 4/24/17.
 */

public class NetworkStateReceiver extends BroadcastReceiver {
    AppRTC_Common.ICall event;
    String TAG=AppRTC_Common.TAG_COMM+"NetworkState";

    public NetworkStateReceiver(AppRTC_Common.ICall event) {
        this.event = event;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "网络状态发生变化");
        //获得ConnectivityManager对象
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();

        if (info != null) {
            //event.onNetworkChange(info.isAvailable());
        }else {
            //event.onNetworkChange(false);
        }
    }
}
