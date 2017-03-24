package com.example.guan.webrtc_android_8.common;

import android.util.Log;

import com.example.guan.webrtc_android_8.utils.AsyncHttpURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.guan.webrtc_android_8.activity.CallActivity.reportError;

/**
 * Created by guan on 3/18/17.
 */

public class Helpers {
    public static String TAG = AppRTC_Common.TAG_COMM + "Helpers";

    private static final int TURN_HTTP_TIMEOUT_MS = 5000;


    /**
     * 注意：获取TurnServer的时候也用到了"https://appr.tc"。到时候要根据实际情况修改
     *
     * @param url
     * @return
     * @throws IOException
     * @throws JSONException
     */
    // Requests & returns a TURN ICE Server based on a request URL.  Must be run
    // off the main thread!
    public static LinkedList<PeerConnection.IceServer> requestTurnServers(String url)
            throws IOException, JSONException {

        //这句话仅仅为了测试
        url = AppRTC_Common.turnServer_URL;

        LinkedList<PeerConnection.IceServer> turnServers = new LinkedList<PeerConnection.IceServer>();
        Log.d(TAG, "Request TURN from: " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("REFERER", AppRTC_Common.WebRTC_URL);
        connection.setConnectTimeout(TURN_HTTP_TIMEOUT_MS);
        connection.setReadTimeout(TURN_HTTP_TIMEOUT_MS);
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            Log.e(TAG,"======requestTurnServers FAILED========");
            throw new IOException("Non-200 response when requesting TURN server from " + url + " : "
                    + connection.getHeaderField(null));
        }
        InputStream responseStream = connection.getInputStream();
        String response = drainStream(responseStream);
        connection.disconnect();
        Log.d(TAG, "TURN response: " + response);
        JSONObject responseJSON = new JSONObject(response);
        JSONArray iceServers = responseJSON.getJSONArray("iceServers");
        for (int i = 0; i < iceServers.length(); ++i) {
            JSONObject server = iceServers.getJSONObject(i);
            JSONArray turnUrls = server.getJSONArray("urls");
            String username = server.has("username") ? server.getString("username") : "";
            String credential = server.has("credential") ? server.getString("credential") : "";
            for (int j = 0; j < turnUrls.length(); j++) {
                String turnUrl = turnUrls.getString(j);
                turnServers.add(new PeerConnection.IceServer(turnUrl, username, credential));
            }
        }
        return turnServers;
    }

    // Return the contents of an InputStream as a String.
    private static String drainStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * 生成SessionDescription中的description
     */
    public static String preferCodec(
            String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length)
                && (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
            }
        }
        if (mLineIndex == -1) {
            Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec);
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at "
                + lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        if (origMLineParts.length > 3) {
            StringBuilder newMLine = new StringBuilder();
            int origPartIndex = 0;
            // Format is: m=<media> <port> <proto> <fmt> ...
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(codecRtpMap);
            for (; origPartIndex < origMLineParts.length; origPartIndex++) {
                if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(origMLineParts[origPartIndex]);
                }
            }
            lines[mLineIndex] = newMLine.toString();
            Log.d(TAG, "Change media description: " + lines[mLineIndex]);
        } else {
            Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    /**
     * 本方法目的只是向服务器发送，并不注重返回的消息
     *
     * @param messageType
     * @param url
     * @param message
     * @param roomId
     */
    public static void sendPostMessage(
            final AppRTC_Common.MessageType messageType,
            final String url,
            final String message,
            final String roomId) {
        String logInfo = url;
        if (message != null) {
            logInfo += ". Message: " + message;
        }
        Log.d(TAG, "C->GAE: " + logInfo);
        AsyncHttpURLConnection httpConnection = new AsyncHttpURLConnection(
                "POST", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
            @Override
            public void onHttpError(String errorMessage) {
                Log.e(TAG, "onHttpError:" + errorMessage);
            }

            @Override
            public void onHttpComplete(String response) {
                if (messageType == AppRTC_Common.MessageType.MESSAGE) {
                    try {
                        JSONObject roomJson = new JSONObject(response);
                        String result = roomJson.getString("result");
                        if (!result.equals("SUCCESS")) {
                            reportError(roomId, "GAE POST error: " + result);
                        }
                    } catch (JSONException e) {
                        reportError(roomId, "GAE POST JSON error: " + e.toString());
                    }
                }
            }
        });
        httpConnection.send();
    }


    public static String createInstanceId() {
        int m = 1000;
        int n = 9999;
        int random = (int) (m + Math.random() * (n - m + 1));
        Log.e(TAG, "createInstanceId: " + random);
        return Integer.toString(random);
    }
}
