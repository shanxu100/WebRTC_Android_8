/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.example.guan.webrtc_android_8.common;

import android.os.Handler;
import android.util.Log;

import com.example.guan.webrtc_android_8.utils.AsyncHttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;


/**
 * WebSocket client implementation.
 * <p>
 * WebSocket是WebRTC的基础，WebSocket为WebRTC负责客服端发现和数据转发
 * 这个类对websocket进行了封装，使其能够对信令服务器传递一些参数，并且接收推送的参数。
 * <p>
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */

public class WebSocketClient {
    private static final String TAG = AppRTC_Common.TAG_COMM + "WSClient";
    private static final int CLOSE_TIMEOUT = 1000;
    private IWebSocketMessageEvent events;
    private Handler handler;
    private WebSocketConnection ws;
    private WebSocketObserver wsObserver;
    private String wsServerUrl;
    private String postServerUrl;
    private String roomID;
    private String clientID;
    private WebSocketConnectionState state;
    private final Object closeEventLock = new Object();
    private boolean closeEvent;
    // WebSocket send queue. Messages are added to the queue when WebSocket
    // client is not registered and are consumed in register() call.
    private final LinkedList<String> wsSendQueue;


    /**
     * Possible WebSocket connection states.
     */
    public enum WebSocketConnectionState {
        NEW, CONNECTED, REGISTERED, CLOSED, ERROR
    }

    /**
     * Callback interface for messages delivered on WebSocket.
     * All events are dispatched from a looper executor thread.
     * 定义主体对WebSocket响应的事件接口。
     * 问题：既然已经有了对webSocket监听的接口WebSocketConnectionObserver，为什么还要自己定义一个接口供主体使用？
     * 主体直接实现WebSocketConnectionObserver接口不更简单吗？
     * 答案：WebSocketConnectionObserver在本类中实现，多记录了一些状态维护的信息，对WebSocket的功能进行了更加完善的封装。
     * 主体实现了自定义的接口，可以使主体更加专注于信息的处理，而不必担心对WebSocket的信息进行维护。维护的这一部分任务，交给本类就好了。
     */
    public interface IWebSocketMessageEvent {
        void onWebSocketMessage(final String message);

        void onWebSocketClose();

        void onWebSocketError(final String description);
    }

    /**
     * 构造函数
     *
     * @param handler
     */
    public WebSocketClient(Handler handler, IWebSocketMessageEvent events, String roomID) {
        this.handler = handler;
        this.roomID = roomID;
        clientID = null;
        wsSendQueue = new LinkedList<String>();
        state = WebSocketConnectionState.NEW;
        this.events = events;
    }

    public WebSocketConnectionState getState() {
        return state;
    }


    public void connect(final String wsUrl, final String postUrl) {
        checkIfCalledOnValidThread();
        if (state != WebSocketConnectionState.NEW) {
            Log.e(TAG, "WebSocket is already connected.");
            return;
        }
        wsServerUrl = wsUrl;
        postServerUrl = postUrl;
        closeEvent = false;

        Log.d(TAG, "Connecting WebSocket to: " + wsUrl + ". Post URL: " + postUrl);
        ws = new WebSocketConnection();
        wsObserver = new WebSocketObserver();
        try {
            ws.connect(new URI(wsServerUrl), wsObserver);
            Log.d(TAG, "Success to Connect WebSocket!");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            //reportError(roomID, "URI error: " + e.getMessage());
        } catch (WebSocketException e) {
            e.printStackTrace();
            //reportError(roomID, "WebSocket connection error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(final String roomID, final String clientID) {
        checkIfCalledOnValidThread();
        this.roomID = roomID;
        this.clientID = clientID;
        if (state != WebSocketConnectionState.CONNECTED) {
            Log.w(TAG, "WebSocket register() in state " + state);
            return;
        }
        Log.d(TAG, "Registering WebSocket for room " + roomID + ". ClientID: " + clientID);
        JSONObject json = new JSONObject();
        try {
            json.put("cmd", "register");
            json.put("roomid", roomID);
            json.put("clientid", clientID);
            Log.d(TAG, "C->WSS: " + json.toString());
            ws.sendTextMessage(json.toString());
            state = WebSocketConnectionState.REGISTERED;
            // Send any previously accumulated messages.
            for (String sendMessage : wsSendQueue) {
                send(sendMessage);
            }
            wsSendQueue.clear();
            Log.e(TAG, "Success to Register WebSocket!");
        } catch (JSONException e) {
            e.printStackTrace();
            //reportError(roomID, "WebSocket register JSON error: " + e.getMessage());
        }
    }

    /**
     * 这里发送的消息是要推送给其他用户的消息，重点是“推送”
     *
     * @param message
     */
    public void send(String message) {
        checkIfCalledOnValidThread();
        switch (state) {
            case NEW:
            case CONNECTED:
                // Store outgoing messages and send them after websocket client
                // is registered.
                //good
                Log.d(TAG, "WS ACC: " + message);
                wsSendQueue.add(message);
                return;
            case ERROR:
            case CLOSED:
                Log.e(TAG, "WebSocket send() in error or closed state : " + message);
                return;
            case REGISTERED:
                JSONObject json = new JSONObject();
                try {
                    json.put("cmd", "send");
                    json.put("msg", message);
                    message = json.toString();
                    Log.d(TAG, "C->WSS: " + message);
                    ws.sendTextMessage(message);
                } catch (JSONException e) {
                    //reportError(roomID, "WebSocket send JSON error: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
        }
    }

    // This call can be used to send WebSocket messages before WebSocket
    // connection is opened.
    public void post(String message) {
        checkIfCalledOnValidThread();
        sendWSSMessage("POST", message);
    }

    public void disconnect(boolean waitForComplete) {
        checkIfCalledOnValidThread();
        Log.d(TAG, "Disconnect WebSocket. State: " + state);
        if (state == WebSocketConnectionState.REGISTERED) {
            // Send "bye" to WebSocket server.
            //send("{\"type\": \"bye\"}");


            state = WebSocketConnectionState.CONNECTED;
            // Send http DELETE to http WebSocket server.
            sendWSSMessage("DELETE", "");
        }
        // Close WebSocket in CONNECTED or ERROR states only.
        if (state == WebSocketConnectionState.CONNECTED || state == WebSocketConnectionState.ERROR) {
            ws.disconnect();
            state = WebSocketConnectionState.CLOSED;

            // Wait for websocket close event to prevent websocket library from
            // sending any pending messages to deleted looper thread.
            if (waitForComplete) {
                synchronized (closeEventLock) {
                    while (!closeEvent) {
                        try {
                            closeEventLock.wait(CLOSE_TIMEOUT);
                            break;
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Wait error: " + e.toString());
                        }
                    }
                }
            }
        }
        Log.e(TAG, "Disconnecting WebSocket done.");
    }


    /**
     * 这里发送的消息是给WebSocket服务器的，即让WebSocket服务器根据发送的消息做出响应。并不推送给用户。
     *
     * @param method
     * @param message
     */
    // Asynchronously send POST/DELETE to WebSocket server.
    private void sendWSSMessage(final String method, final String message) {
        String postUrl = postServerUrl + "/" + roomID + "/" + clientID;
        Log.d(TAG, " " + method + " : " + postUrl + " : " + message);
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection(method, postUrl, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        //reportError(roomID, "WS " + method + " error: " + errorMessage);
                        Log.e(TAG, "WS " + method + " error: " + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                    }
                });
        httpConnection.send();
    }

    // Helper method for debugging purposes. Ensures that WebSocket method is
    // called on a looper thread.
    private void checkIfCalledOnValidThread() {

        if (Thread.currentThread() != handler.getLooper().getThread()) {
            Log.e(TAG, "WebSocket method is not called on valid thread");
            throw new IllegalStateException("WebSocket method is not called on valid thread");
        }
    }


    //===========================================================================

    /**
     * 内部类：用于具体实现对WebSocket的监听
     */
    private class WebSocketObserver implements WebSocket.WebSocketConnectionObserver {
        @Override
        public void onOpen() {
            Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    state = WebSocketConnectionState.CONNECTED;
                    // Check if we have pending register request.
                    if (roomID != null && clientID != null) {
                        register(roomID, clientID);
                    }
                }
            });
        }

        @Override
        public void onClose(WebSocketCloseNotification code, String reason) {
            Log.d(TAG, "WebSocket connection closed. Code: " + code + ". Reason: " + reason + ". State: "
                    + state);
            synchronized (closeEventLock) {
                closeEvent = true;
                closeEventLock.notify();
            }

//            handler.post(new Runnable() {
//                @Override
//                public void run() {
            if (state != WebSocketConnectionState.CLOSED) {
                state = WebSocketConnectionState.CLOSED;
                events.onWebSocketClose();
            }
//                }
//            });
        }

        /**
         * 这个方法重要，用于接收服务器推送过来的消息
         *
         * @param payload
         */
        @Override
        public void onTextMessage(String payload) {
            Log.d(TAG, "WSS->C: " + payload);
            final String message = payload;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (state == WebSocketConnectionState.CONNECTED
                            || state == WebSocketConnectionState.REGISTERED) {
                        events.onWebSocketMessage(message);
                    }
                }
            });

        }

        @Override
        public void onRawTextMessage(byte[] payload) {
        }

        @Override
        public void onBinaryMessage(byte[] payload) {
        }
    }


}
