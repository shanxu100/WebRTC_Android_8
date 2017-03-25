package com.example.guan.webrtc_android_8.common;

import android.os.Handler;
import android.util.Log;

import com.example.guan.webrtc_android_8.activity.CallActivity;

import java.util.HashMap;
import java.util.Map;

import static com.example.guan.webrtc_android_8.common.Helpers.createInstanceId;

/**
 * Created by guan on 3/24/17.
 */

public class ClientManager {

    String TAG = AppRTC_Common.TAG_COMM + "ClientManager";

    public final int maxInstance;
    public final String clientId;
    HashMap<String, InstanceManager> map_instaces = new HashMap<>();

    private Handler handler;

    public ClientManager(int maxInstance,
                         AppRTC_Common.SignalingParameters sigParms,
                         AppRTC_Common.RoomConnectionParameters connectionParameters,Handler hander) {
        this.maxInstance = maxInstance;
        this.clientId = sigParms.clientId;
        this.handler = handler;

        initInstanceManager(sigParms, connectionParameters);
    }

    //=====================================

    public HashMap<String, InstanceManager> getMap_instaces() {
        return map_instaces;
    }


    //=====================================

    private void initInstanceManager(AppRTC_Common.SignalingParameters sigParms,
                                     AppRTC_Common.RoomConnectionParameters connectionParameters) {
        for (int i = 0; i < maxInstance; i++) {

            String instanceId = createInstanceId();
            if (map_instaces.containsKey(instanceId)) {
                i--;
                Log.e(TAG, "生成重复的instanceId。重新初始化");
                continue;
            }
            InstanceManager instanceManager = new InstanceManager(connectionParameters,
                    sigParms,
                    instanceId,
                    handler);

            map_instaces.put(instanceManager.getLocalInstanceId(), instanceManager);

        }

        Log.d(TAG, "初始化多个Instance成功----map_instaces.size() = " + map_instaces.size());
    }

    public InstanceManager getInstanceManager(String instanceId) {
        if (map_instaces.containsKey(instanceId)) {
            return map_instaces.get(instanceId);
        } else {
            //Log.e(TAG, "没有找到该实例，instanceId=" + instanceId);
            return null;
        }
    }


}
