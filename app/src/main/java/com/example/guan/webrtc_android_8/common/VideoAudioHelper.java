package com.example.guan.webrtc_android_8.common;

import android.content.Context;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

/**
 * Created by guan on 2/25/17.
 * <p>
 * 创建本地视频音频流
 */

public class VideoAudioHelper {

    String TAG = AppRTC_Common.TAG_COMM + "VideoAudioHelper";
    Context context;

    CameraVideoCapturer videoCapturer;
    VideoSource videoSource;
    AudioSource audioSource;

    // enableVideo is set to true if video should be rendered and sent.
    private boolean renderVideo = true;

    // enableAudio is set to true if audio should be sent.
    private boolean enableAudio = true;

    private PeerConnectionFactory factory;
    private SurfaceViewRenderer localRender;

    public VideoAudioHelper(Context context, PeerConnectionFactory factory, SurfaceViewRenderer localRender) {
        this.factory = factory;
        this.context = context;
        this.localRender = localRender;

        videoCapturer = createVideoCapturer(new Camera2Enumerator(context));
    }


    public void createLocalMedia() {
        String name = "";
        //id随便了
        final String VIDEO_TRACK_ID = "local_video_track";
        final String AUDIO_TRACK_ID = "local_audio_track";

        VideoTrack localVideoTrack;
        AudioTrack localAudioTrack;
        MediaConstraints audioConstraints;

        //同一个videosource，添加到多个videotrack
        videoSource = factory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(50, 50, 40);
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to open camera：videoCapturer is null");
            return;
        }
        //remote视频和音频在PCObserver的onAddStream中设置的
        localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(renderVideo);//true
        localVideoTrack.addRenderer(new VideoRenderer(localRender));//将视频轨道加入到控件中

        audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("levelControl", "true"));
        audioSource = factory.createAudioSource(audioConstraints);
        //localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        //localAudioTrack.setEnabled(enableAudio);

    }


    /**
     * 创建视频流和音频流
     */
    public MediaStream createMediaStream(String name) {

        /**
         * capturer --> source --> track --> stream
         */

        //id随便了
        final String VIDEO_TRACK_ID = "video_track_" + name;
        final String AUDIO_TRACK_ID = "audio_track_" + name;
        final String MEDIA_STREAM_NAME = "ARDAMS" + name;

        MediaStream mediaStream;
        VideoTrack videoTrack;
        AudioTrack audioTrack;
        MediaConstraints audioConstraints;

        //同一个videosource，添加到多个videotrack
        videoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrack.setEnabled(renderVideo);//true


        audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("levelControl", "true"));
        //AudioSource audioSource = factory.createAudioSource(audioConstraints);
        audioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        audioTrack.setEnabled(enableAudio);

        //将视频轨道和音频轨道加入到stream中
        try {
            mediaStream = factory.createLocalMediaStream(MEDIA_STREAM_NAME);
            if (mediaStream == null) {
                Log.e(TAG, "mediaStream is null");
                return null;
            } else {
                mediaStream.addTrack(videoTrack);
                //mediaStream.addTrack(audioTrack);

                return mediaStream;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    private CameraVideoCapturer createVideoCapturer(CameraEnumerator enumerator) {

        CameraVideoCapturer videoCapturer;

        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.e(TAG, "Creating front facing camera capturer.");
                videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.e(TAG, "Creating other camera capturer.");
                videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void close() {

//        Log.d(TAG, "Closing mediaStream.");
//            if (mediaStream != null) {
//                mediaStream.dispose();
//                mediaStream = null;
//            }

        Log.e(TAG, "Close video audio helper");
        try {

            Log.d(TAG, "Closing video source.");
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            Log.d(TAG, "Closing audio source.");
            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }
            Log.d(TAG, "Stopping capture.");
            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                videoCapturer.dispose();
                videoCapturer = null;
            }
            Log.d(TAG, "Close video audio helper——DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
