package com.example.guan.webrtc_android_8.common;

import android.os.Handler;
import android.util.Log;

import com.example.guan.webrtc_android_8.activity.CallActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.LinkedList;


import static com.example.guan.webrtc_android_8.activity.CallActivity.reportError;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.AUDIO_CODEC_ISAC;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.ROOM_LEAVE;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.ROOM_MESSAGE;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.preferredVideoCodec;
import static com.example.guan.webrtc_android_8.common.Helpers.preferCodec;
import static com.example.guan.webrtc_android_8.common.Helpers.sendPostMessage;
import static com.example.guan.webrtc_android_8.common.JsonHelper.jsonPut;
import static com.example.guan.webrtc_android_8.common.JsonHelper.toJsonCandidate;

/**
 * Created by guan on 3/18/17.
 */

public class PeerManager {

    private String localInstanceId;
    //private String remoteInstanceId;//设置到了SignalingParameters中

    private String roomId;
    private String messageUrl;
    private String leaveUrl;
    private AppRTC_Common.RoomState roomState;
    private boolean isInitiator;
    private AppRTC_Common.SignalingParameters sigParms;
    private LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<>();

    private LinkedList<IceCandidate> localQueuedRemoteCandidates_backup = new LinkedList<>();
    private SessionDescription localSdp_backup;


    private PeerConnection peerConnection;
    private WebSocketClient wsClient;
    private CallActivity.WSMessageEvent wsMessageEvent;
    private CallActivity.PCObserver pcObserver;
    private CallActivity.SDPObserver sdpObserver;

    private SurfaceViewRenderer remoteRenderer;
    private MediaStream mediaStream;

    AppRTC_Common.ICall iCall;

    private Handler handler;

    private String TAG = AppRTC_Common.TAG_COMM + "PeerManager";


    public PeerManager(AppRTC_Common.RoomConnectionParameters connectionParameters,
                       AppRTC_Common.SignalingParameters sigParms,
                       String localInstanceId,
                       AppRTC_Common.ICall iCall,
                       Handler handler) {
        this.roomId = sigParms.roomId;
        this.isInitiator = sigParms.initiator;
        this.messageUrl = connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.roomId
                + "/" + sigParms.clientId + "/" + localInstanceId;
        this.leaveUrl = connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.roomId + "/"
                + sigParms.clientId + "/" + localInstanceId;
        this.roomState = AppRTC_Common.RoomState.CONNECTED;

        this.sigParms = sigParms;
        this.iCall = iCall;
        this.handler = handler;
        this.localInstanceId = localInstanceId;


        /**
         *从信令服务器中获得的一系列参数中，有一个是clientId :
         *  clientId是构成messageUrl的一部分，而messageUrl是客户端发送offer的地址；
         *  每个客户端，即使进入同一个房间（即输入同一个房间号roomId），但是信令服务器返回的clientId也是不同的。
         */
        Log.d(TAG, "Message URL: " + this.messageUrl);
        Log.d(TAG, "Leave URL: " + this.leaveUrl);

    }

    private void initParams() {

    }
    //======================

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public WebSocketClient getWsClient() {
        return wsClient;
    }

    public void setWsClient(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public CallActivity.PCObserver getPcObserver() {
        return pcObserver;
    }

    public void setPcObserver(CallActivity.PCObserver pcObserver) {
        //这里要保证remoteRenderer和wsClient已经被实例化
        this.pcObserver = pcObserver;
    }

    public CallActivity.SDPObserver getSdpObserver() {
        return sdpObserver;
    }

    public void setSdpObserver(CallActivity.SDPObserver sdpObserver) {
        this.sdpObserver = sdpObserver;
    }

    public SurfaceViewRenderer getRemoteRenderer() {
        return remoteRenderer;
    }

    public void setRemoteRenderer(SurfaceViewRenderer remoteRenderer) {
        this.remoteRenderer = remoteRenderer;
    }

    public AppRTC_Common.RoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(AppRTC_Common.RoomState roomState) {
        this.roomState = roomState;
    }

    public CallActivity.WSMessageEvent getWsMessageEvent() {
        return wsMessageEvent;
    }

    public void setWsMessageEvent(CallActivity.WSMessageEvent wsMessageEvent) {
        this.wsMessageEvent = wsMessageEvent;
    }

    public LinkedList<IceCandidate> getQueuedRemoteCandidates() {
        return queuedRemoteCandidates;
    }

    public void setQueuedRemoteCandidates(LinkedList<IceCandidate> queuedRemoteCandidates) {
        this.queuedRemoteCandidates = queuedRemoteCandidates;
    }

    public SessionDescription getLocalSdp_backup() {
        return localSdp_backup;
    }

    public void setLocalSdp_backup(SessionDescription localSdp_backup) {
        this.localSdp_backup = localSdp_backup;
    }

    public MediaStream getMediaStream() {
        return mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    //===============================


    public String getLocalInstanceId() {
        return localInstanceId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getMessageUrl() {
        return messageUrl;
    }

    public String getLeaveUrl() {
        return leaveUrl;
    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public AppRTC_Common.SignalingParameters getSigParms() {
        return sigParms;
    }

    public LinkedList<IceCandidate> getLocalQueuedRemoteCandidates_backup() {
        return localQueuedRemoteCandidates_backup;
    }


//=====================================================

    /**
     * 以下是回调方法
     */
    //=====================================================

    /**
     * 断开链接
     */
    public void disconnect() {
        Log.e(TAG, "===========================Disconnect==================");

        //disconnectRoomForInstance();

        Log.e(TAG, "Closing room: " + roomId +"\tCloseing instance: "+localInstanceId+ "\tRoom state: " + roomState);
        if (roomState == AppRTC_Common.RoomState.CONNECTED) {
            //和房间服务器断开链接
            sendPostMessage(AppRTC_Common.MessageType.LEAVE,
                    leaveUrl,
                    null,
                    roomId);
        }

        Log.e(TAG, "Closing peer connection.");
        if (peerConnection != null) {
            //peerConnection.removeStream(mediaStream);
            peerConnection.dispose();
        }

        Log.e(TAG, "====Closing Remote Render===");
        if (remoteRenderer != null) {
            //n人视频，会创建n-1个remoteRender。每个对象都要release
            remoteRenderer.release();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                WebSocketSendBye();
            }
        });

        Log.e(TAG, "=====Closing mediaStream.======");
        if (mediaStream != null) {
            mediaStream.dispose();
            mediaStream = null;
        }

        //iCall.onClose(remoteRenderer, roomId);

    }

    private void disconnectRoomForInstance() {

    }

    private void WebSocketSendBye() {

        Log.e(TAG,"======WebSocketSendBye======");
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "bye");
        jsonPut(json, "receiverId", sigParms.remoteInstanceId);
        jsonPut(json, "senderId", localInstanceId);
        Log.e(TAG, "WebSocket Send Bye: " + json.toString());
        wsClient.send(json.toString());
    }

    //==============配合PCObserver使用的方法===========================
    // Send Ice candidate to the other participant.
    public void sendLocalIceCandidate(final IceCandidate candidate) {

        Log.e(TAG,"send Local IceCandidate");

        JSONObject json = new JSONObject();

        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);


        if (isInitiator) {
            // Call initiator sends ice candidates to GAE server.
            if (roomState != AppRTC_Common.RoomState.CONNECTED) {
                Log.e(TAG, "Sending ICE candidate in non connected state.");
                return;
            }
            sendPostMessage(AppRTC_Common.MessageType.MESSAGE,
                    messageUrl,
                    json.toString(),
                    roomId);

        } else {
            // Call receiver sends ice candidates to websocket server.
            jsonPut(json, "receiverId", sigParms.remoteInstanceId);
            jsonPut(json, "senderId", localInstanceId);
            wsClient.send(json.toString());
        }

    }

    // Send removed Ice candidates to the other participant.
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {

        Log.e(TAG,"send Local IceCandidate Removals");
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
            //删除备份
            localQueuedRemoteCandidates_backup.remove(candidate);

            jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);
        //jsonPut(json, "instanceId", sigParms.remoteInstanceId);

        if (isInitiator) {
            // Call initiator sends ice candidates to GAE server.
            if (roomState != AppRTC_Common.RoomState.CONNECTED) {
                Log.e(TAG, "Sending ICE candidate removals in non connected state.");
                return;
            }
            sendPostMessage(AppRTC_Common.MessageType.MESSAGE,
                    messageUrl,
                    json.toString(),
                    roomId);

        } else {
            // Call receiver sends ice candidates to websocket server.
            jsonPut(json, "receiverId", sigParms.remoteInstanceId);
            jsonPut(json, "senderId", localInstanceId);
            wsClient.send(json.toString());
        }

    }


    public void addRemoteIceCandidate(IceCandidate candidate) {
        // Log.e(TAG, "添加远端Ice:addRemoteIceCandidate--------" + candidate);
        if (peerConnection != null) {

            if (queuedRemoteCandidates != null && queuedRemoteCandidates.size() != 0) {
                queuedRemoteCandidates.add(candidate);
            } else {
                peerConnection.addIceCandidate(candidate);
            }
        }

    }


    public void removeRemoteIceCandidates(IceCandidate[] candidates) {
        Log.e(TAG, "删除远端Ice:removeRemoteIceCandidates--------");

        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                peerConnection.addIceCandidate(candidate);
            }
            //queuedRemoteCandidates = null;
            queuedRemoteCandidates.clear();
        }
        for (IceCandidate ice : candidates) {
            Log.d(TAG, "ice : " + ice);
        }
        peerConnection.removeIceCandidates(candidates);
    }


    //==============配合SdpObserver使用的方法===========================
    public void setLocalDescription(final SessionDescription sdp) {

        if (peerConnection != null) {
            Log.d(TAG, "Set local SDP from " + sdp.type);

            //保存本地的SessionDescription
            peerConnection.setLocalDescription(sdpObserver, sdp);
        }

    }

    public void setRemoteDescription(final SessionDescription sdp) {

        Log.e(TAG, "localInstanceId:"+localInstanceId+"remoteInstanceId:"+sigParms.remoteInstanceId+
                "===setRemoteDescription===");
        if (peerConnection == null) {
            return;
        }
        String sdpDescription = sdp.description;

        sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
        sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false);

        Log.d(TAG, "Set remote SDP.");
        SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
        peerConnection.setRemoteDescription(sdpObserver, sdpRemote);
    }


    /**
     * Send local offer SDP to the other participant.
     * 将本地的 offer sdp 发送给其他的参与者
     *
     * @param sdp
     */
    public void sendOfferSdp(final SessionDescription sdp) {

        if (roomState != AppRTC_Common.RoomState.CONNECTED) {
            reportError(roomId, "Sending offer SDP in non connected state.");
            return;
        }



        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        //jsonPut(json, "instanceId", localInstanceId);

        //加入clientID
        //====
        Log.e(TAG, "send OfferSdp: " + json.toString());

        sendPostMessage(AppRTC_Common.MessageType.MESSAGE,
                messageUrl,
                json.toString(),
                roomId);
    }

    /**
     * Send local answer SDP to the other participant.
     * 将本地的 answer sdp 发送给其他的参与者
     *
     * @param sdp
     */
    public void sendAnswerSdp(final SessionDescription sdp) {

        //做备份
        localSdp_backup = sdp;

        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        jsonPut(json, "receiverId", sigParms.remoteInstanceId);
        jsonPut(json, "senderId", localInstanceId);
        Log.e(TAG, "send AnswerSdp: " + json.toString());

        wsClient.send(json.toString());

    }

    /**
     * 问题：同样是send offer/answer , 为什么采取的发送方式不同：send offer时使用的是http；send answer时使用的是websocket
     * <p>
     * 答：发送的地址不一样。offer发送到messageUrl地址中，answer发送到WebSocket服务器中。而WebSocket服务器的地址是在connect/register确定的
     */

    //====================================================
    public void resendLocalSdp() {
        if (isInitiator()) {
            sendOfferSdp(localSdp_backup);
        } else {
            sendAnswerSdp(localSdp_backup);
        }
    }

    public void resendLocalIceCandidate() {
        Log.e(TAG,"localQueuedRemoteCandidates_backup.size: "+localQueuedRemoteCandidates_backup.size());
        for (IceCandidate candidate : localQueuedRemoteCandidates_backup) {
            sendLocalIceCandidate(candidate);
        }
    }


}