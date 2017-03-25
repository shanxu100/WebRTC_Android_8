/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.example.guan.webrtc_android_8.common;

import android.util.Log;

import com.example.guan.webrtc_android_8.activity.CallActivity;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;

/**
 * AppRTCClient is the interface representing an AppRTC client.
 */
public class AppRTC_Common {

    //定义
    public enum RoomState {
        NEW, CONNECTED, CLOSED, ERROR
    }

    public enum RoomRole {
        MASTER, SLAVE
    }

    public enum MessageType {MESSAGE, LEAVE}

    //描述
    public static final String ROOM_JOIN = "join";
    public static final String ROOM_MESSAGE = "message";
    public static final String ROOM_LEAVE = "leave";
    public static final String ROOM_QUERY = "query";
    public static final String ROOM_M_TO_M = "0";//多对多
    public static final String ROOM_P_TO_M = "1";//一对多


    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";
    public static final String preferredVideoCodec = VIDEO_CODEC_VP8;

    //服务器设置选项;
    public static final String aliyun_WebRTC_URL = "https://i-test.com.cn";
    public static final String aliyun_turnServer_URL = "https://i-test.com.cn/iceconfig.php";

    public static final String labServer_WebRTC_URL = "https://222.201.145.167";
    public static final String labServer_turnServer_URL = "https://222.201.145.167/iceconfig.php";

    public static String WebRTC_URL = aliyun_WebRTC_URL;
    public static String turnServer_URL = aliyun_turnServer_URL;

    //模式设置选项
    public static String P_2_M="Point to Multipoint";
    public static String M_2_M="Multipoint to Multipoint";
    public static String selectedMode=M_2_M;


    //public static final String WebRTCUrl = WebRTC_URL;
    public static final String HTTP_ORIGIN = WebRTC_URL;
    public static final String TAG_COMM = "appRTC-";

    private static String TAG = AppRTC_Common.TAG_COMM + "AppRTC_Common";

    /**
     * Struct holding the connection parameters of an AppRTC room.
     * 定义发起链接的参数
     */
    public static class RoomConnectionParameters {
        public final String roomUrl;
        public final String roomId;
        //public final String roomType;

        @Override
        public String toString() {
            return "roomUrl:" + roomUrl
                    + "\troomId:" + roomId;
        }

        public RoomConnectionParameters(String roomUrl, String roomId) {
            this.roomUrl = roomUrl;
            this.roomId = roomId;
        }
    }

    /**
     * Struct holding the signaling parameters of an AppRTC room.
     * <p>
     * 定义从信令服务器得到的参数
     */
    public static class SignalingParameters {
        public final String result;
        public final List<PeerConnection.IceServer> iceServers;
        //public final boolean initiator;
        public final String clientId;
        public String roomSize;
        public final String wssUrl;
        public final String wssPostUrl;
        public final SessionDescription offerSdp;
        public final List<IceCandidate> iceCandidates;
        public final String roomId;

        public SignalingParameters(String result, List<PeerConnection.IceServer> iceServers, boolean initiator,
                                   String clientId, String wssUrl, String wssPostUrl, SessionDescription offerSdp,
                                   List<IceCandidate> iceCandidates, String roomId, String roomSize) {
            this.result = result;
            this.iceServers = iceServers;
            //this.initiator = initiator;
            this.clientId = clientId;
            this.roomSize = roomSize;
            this.wssUrl = wssUrl;
            this.wssPostUrl = wssPostUrl;
            this.offerSdp = offerSdp;
            this.iceCandidates = iceCandidates;
            this.roomId = roomId;

        }

        @Override
        public String toString() {
            return "SignalingParameters{" +
                    "iceServers=" + iceServers.get(0) +
                    //", initiator=" + initiator +
                    ", clientId='" + clientId + '\'' +
                    //", roomSize='" + roomSize + '\'' +
                    ", wssUrl='" + wssUrl + '\'' +
                    ", wssPostUrl='" + wssPostUrl + '\'' +
                    ", offerSdp=" + offerSdp +
                    ", iceCandidates=" + iceCandidates +
                    ", roomId=" + roomId +
                    '}';
        }
    }

    public static class MessageParameters {
        public final String roomId;
        public final String clientId;
        public String remoteInstanceId;
        public final SessionDescription offerSdp;
        public final List<IceCandidate> iceCandidates;

        public MessageParameters(String roomId, String clientId,
                                 String remoteInstanceId,
                                 SessionDescription offerSdp,
                                 List<IceCandidate> iceCandidates) {
            this.roomId = roomId;
            this.clientId = clientId;
            this.remoteInstanceId = remoteInstanceId;
            this.offerSdp = offerSdp;
            this.iceCandidates = iceCandidates;
        }

        @Override
        public String toString() {
            return "MessageParameters{" +
//                    "roomID=" + roomId +
//                    ", clientId='" + clientId + '\'' +
                    ", remoteInstanceId=" + remoteInstanceId +
                    ", offerSdp=" + offerSdp +
                    ", iceCandidates=" + iceCandidates +
                    '}';
        }
    }


    //=========================================

    /**
     * 定义从服务器轮询后得到的新的房间号
     */
    public static class BackupRoomParameters {
        public final String result;
        public final String backup;

        public BackupRoomParameters(String result, String backup) {
            this.result = result;
            this.backup = backup;
        }
    }


    public interface ICall {
        void onClose(SurfaceViewRenderer renderer, String roomId);
    }


//==========================================================================
}
