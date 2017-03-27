package com.example.guan.webrtc_android_8.common;

import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import static com.example.guan.webrtc_android_8.common.Helpers.requestTurnServers;

/**
 * Created by guan on 10/20/16.
 */

public class JsonHelper {

    public static String TAG = AppRTC_Common.TAG_COMM + "JsonHelper";

    private static final int TURN_HTTP_TIMEOUT_MS = 5000;


    // Put a |key|->|value| mapping in |json|.
    public static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    public static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    public static IceCandidate toJavaCandidate(JSONObject json) {
        try {
            return new IceCandidate(
                    json.getString("id"),
                    json.getInt("label"),
                    json.getString("candidate")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 解析访问房间服务器后返回的参数
     *
     * @param response
     */
    public static AppRTC_Common.SignalingParameters roomHttpResponseParse(String response) {

        Log.d(TAG, "解析访问房间服务器后返回的参数");

        try {
            LinkedList<IceCandidate> iceCandidates = null;
            SessionDescription offerSdp = null;
            JSONObject roomJson = new JSONObject(response);

            Log.e(TAG, "roomHttpResponseParse: " + roomJson.toString());
            String result = roomJson.getString("result");
            //=================
            if (result.equals("ROOM_FULL")) {
                Log.e(TAG, "ROOM_FULL");
                AppRTC_Common.SignalingParameters params = new AppRTC_Common.SignalingParameters(result,
                        null, false, null, null, null, null, null, null, null);

                return params;

            } else if (result.equals("SUCCESS")) {
                response = roomJson.getString("params");
                roomJson = new JSONObject(response);
                String roomId = roomJson.getString("room_id");
                String clientId = roomJson.getString("client_id");
                String wssUrl = roomJson.getString("wss_url");
                String wssPostUrl = roomJson.getString("wss_post_url");
                String roomSize = roomJson.getString("room_size");
                boolean initiator = (roomJson.getBoolean("is_initiator"));

                Log.d(TAG, "RoomId: " + roomId + ". ClientId: " + clientId);
                Log.d(TAG, "Initiator: " + initiator);
                Log.e(TAG, "roomSize: " + roomSize);
                //Log.d(TAG, "WSS url: " + wssUrl);
                //Log.d(TAG, "WSS POST url: " + wssPostUrl);

                LinkedList<PeerConnection.IceServer> iceServers =
                        iceServersFromPCConfigJSON(roomJson.getString("pc_config"));

                boolean isTurnPresent = false;
                //boolean isTurnPresent = true;
                for (PeerConnection.IceServer server : iceServers) {
                    Log.d(TAG, "IceServer: " + server);
                    if (server.uri.startsWith("turn:")) {
                        isTurnPresent = true;
                        break;
                    }
                }
                // Request TURN servers.
                if (!isTurnPresent) {
                    Log.e(TAG, "======requestTurnServers=========");
                    LinkedList<PeerConnection.IceServer> turnServers =
                            requestTurnServers(roomJson.getString("ice_server_url"));
                    for (PeerConnection.IceServer turnServer : turnServers) {
                        Log.d(TAG, "TurnServer: " + turnServer);
                        iceServers.add(turnServer);
                    }

                }

                AppRTC_Common.SignalingParameters params = new AppRTC_Common.SignalingParameters(result,
                        iceServers, initiator, clientId,
                        wssUrl, wssPostUrl, offerSdp, iceCandidates, roomId, roomSize);

                return params;

            } else {
                Log.e(TAG, "Room response error: " + result);
                return null;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Room JSON parsing error: " + e.toString());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Room IO error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }


    public static AppRTC_Common.MessageParameters messageHttpResponseParameters(String response) {

        try {
            LinkedList<IceCandidate> iceCandidates = new LinkedList<IceCandidate>();
            SessionDescription offerSdp = null;
            JSONObject roomJson = new JSONObject(response);

            Log.e(TAG, "====MessageParameters:==== "+response);

            String result = roomJson.getString("result");
            if (!result.equals("SUCCESS")) {
                Log.e(TAG, "MessageParameters获取失败");
                return null;
            }


            String roomId = roomJson.getString("room_id");
            String clientId = roomJson.getString("client_id");
            String remoteInstanceId = (String) roomJson.get("instance_id");
            String messagesString = roomJson.getString("messages");

            JSONArray messages = new JSONArray(messagesString);
            //Log.e(TAG, "message.length = " + messages.length() + "\troomJson Message : " + messagesString);

            for (int i = 0; i < messages.length(); ++i) {
                String messageString = messages.getString(i);
                JSONObject message = new JSONObject(messageString);
                String messageType = message.getString("type");
                Log.d(TAG, "GAE->C #" + i + " : " + messageString);
                //Log.e(TAG,"Message type : "+messageType);
                if (messageType.equals("offer")) {

                    //Log.e(TAG, "收到sdp offer\t");
                    offerSdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(messageType), message.getString("sdp"));

                } else if (messageType.equals("candidate")) {

                    //Log.e(TAG, "收到candidate\t" + message.getString("candidate"));
                    IceCandidate candidate = new IceCandidate(
                            message.getString("id"), message.getInt("label"), message.getString("candidate"));
                    iceCandidates.add(candidate);

                } else {
                    Log.e(TAG, "Unknown message: " + messageString);
                }
            }

            return new AppRTC_Common.MessageParameters(roomId, clientId,
                    remoteInstanceId, offerSdp, iceCandidates);


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    // Return the list of ICE servers described by a WebRTCPeerConnection
    // configuration string.
    private static LinkedList<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig)
            throws JSONException {
        JSONObject json = new JSONObject(pcConfig);
        JSONArray servers = json.getJSONArray("iceServers");
        LinkedList<PeerConnection.IceServer> ret = new LinkedList<PeerConnection.IceServer>();
        for (int i = 0; i < servers.length(); ++i) {
            JSONObject server = servers.getJSONObject(i);
            String url = server.getString("urls");
            String credential = server.has("credential") ? server.getString("credential") : "";
            ret.add(new PeerConnection.IceServer(url, "", credential));
        }
        return ret;
    }


}
