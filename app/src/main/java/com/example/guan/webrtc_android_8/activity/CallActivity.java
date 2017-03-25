package com.example.guan.webrtc_android_8.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;
import com.example.guan.webrtc_android_8.common.AppRTC_Common;
import com.example.guan.webrtc_android_8.common.ClientManager;
import com.example.guan.webrtc_android_8.common.InstanceManager;
import com.example.guan.webrtc_android_8.common.JsonHelper;
import com.example.guan.webrtc_android_8.common.VideoAudioHelper;
import com.example.guan.webrtc_android_8.common.WebSocketClient;
import com.example.guan.webrtc_android_8.utils.AsyncHttpURLConnection;
import com.example.guan.webrtc_android_8.utils.ClickUtil;
import com.example.guan.webrtc_android_8.utils.ShowUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static com.example.guan.webrtc_android_8.common.AppRTC_Common.AUDIO_CODEC_ISAC;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.ROOM_JOIN;
import static com.example.guan.webrtc_android_8.common.AppRTC_Common.VIDEO_CODEC_VP8;
import static com.example.guan.webrtc_android_8.common.Helpers.preferCodec;
import static com.example.guan.webrtc_android_8.common.JsonHelper.toJavaCandidate;

public class CallActivity extends AppCompatActivity implements AppRTC_Common.ICall {

    //类相关
    private static Context context;
    private static String TAG = AppRTC_Common.TAG_COMM + "CallActivity";

    //UI相关和mediaStream相关
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender_1;
    private SurfaceViewRenderer remoteRender_2;
    private SurfaceViewRenderer remoteRender_3;
    private RelativeLayout renderer_layout;
    private static Stack<SurfaceViewRenderer> stack_AvailableRemoteRender = new Stack<>();
    private Button cancel_btn;
    private Button recover_btn;
    private EglBase rootEglBase;
    private TextView roomID_tv;

    //线程相关
    private ScheduledExecutorService executor;
    private Handler handler;
    private String ThreadTAG = "looperThread_TAG";

    //链接参数相关
    private String roomUrl = "";
    private String roomId = "";

    //连接对象相关
    private PeerConnectionFactory factory;
    private PeerConnectionFactory.Options factoryOpyions;
    private AppRTC_Common.RoomRole role;
    WSMessageEvent wsMessageEvent;
    WebSocketClient wsClient;
    ClientManager clientManager;

    //辅助相关
    VideoAudioHelper vaHelper;
    private int MaxInstance = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        initData();
        initUI();
        initLink();

        AppRTC_Common.RoomConnectionParameters connectionParameters = new AppRTC_Common.RoomConnectionParameters(roomUrl, roomId, false);
        joinRoom(connectionParameters);
    }

    private void initData() {

        context = CallActivity.this;
        roomUrl = getIntent().getStringExtra("roomurl");
        roomId = getIntent().getStringExtra("roomid");
        role = (AppRTC_Common.RoomRole) getIntent().getExtras().get("role");

        /**
         * 学习多线程的知识点
         */
        //初始化一个死循环线程
        final HandlerThread handlerThread = new HandlerThread(ThreadTAG);
        handlerThread.start();//handlerThread.start()之后，才能getLooper()
        handler = new Handler(handlerThread.getLooper());
        //一个单例的executor线程池：创建只有一条线程的线程池，他可以在指定延迟后执行线程任务
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
            }
        });


    }

    private void initUI() {
        Window window = this.getWindow();
        window.setStatusBarColor(Color.BLACK);

        cancel_btn = (Button) this.findViewById(R.id.Cancel_btn);
        recover_btn = (Button) this.findViewById(R.id.recover_btn);
        roomID_tv = (TextView) this.findViewById(R.id.RoomID_tv);
        roomID_tv.setText(roomId);

        //设置视频的框框
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_renderer);
        remoteRender_1 = (SurfaceViewRenderer) findViewById(R.id.remote_renderer_1);
        remoteRender_2 = (SurfaceViewRenderer) findViewById(R.id.remote_renderer_2);
        remoteRender_3 = (SurfaceViewRenderer) findViewById(R.id.remote_renderer_3);
        renderer_layout = (RelativeLayout) findViewById(R.id.renderer_layout);
        int width=renderer_layout.getWidth();
        int height=renderer_layout.getHeight();

        rootEglBase = EglBase.create();
        renderSetting(localRender, true,width,height);
        renderSetting(remoteRender_1, false,width,height);
        renderSetting(remoteRender_2, false,width,height);
        renderSetting(remoteRender_3, false,width,height);

        stack_AvailableRemoteRender.push(remoteRender_3);
        stack_AvailableRemoteRender.push(remoteRender_2);
        stack_AvailableRemoteRender.push(remoteRender_1);

        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击取消按钮，中断连接
                Log.d(TAG, "Click Cancel_btn");
                /**
                 * keySet是键的集合，Set里面的类型即key的类型
                 * entrySet是 键-值 对的集合，Set里面的类型是Map.Entry
                 */

                for (Map.Entry<String, InstanceManager> entry : clientManager.getMap_instaces().entrySet()) {

                    try {
                        entry.getValue().disconnect();
                        Log.e(TAG, "close instanceManager: instanceId=" + entry.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                clientManager.getMap_instaces().clear();
                closeOwn();

            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        };

        remoteRender_1.setOnClickListener(listener);
        remoteRender_2.setOnClickListener(listener);
        remoteRender_3.setOnClickListener(listener);

        recover_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //updateView(remote_renderer_layout_1, remoteRender_1);
                //recoverView();
            }
        });


    }

    private void initLink() {

        //初始化PeerConnectionFactory，以后用于生产PeerConnection
        try {
            PeerConnectionFactory.initializeInternalTracer();
            PeerConnectionFactory.initializeFieldTrials("");
            if (!PeerConnectionFactory.initializeAndroidGlobals(
                    context, true, true, true)) {
                Log.e(TAG, "Failed to initializeAndroidGlobals");
            }
            factoryOpyions = new PeerConnectionFactory.Options();
            factoryOpyions.networkIgnoreMask = 0;
            factory = new PeerConnectionFactory(factoryOpyions);
            //加上这句话才显示了图像
            factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
            Log.d(TAG, "Peer connection factory created.");

            vaHelper = new VideoAudioHelper(context, factory, localRender);
            vaHelper.createLocalMedia();


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //===========================================================

    /**
     * 负责对render进行初始化和设置
     *
     * @param renderer
     */
    private void renderSetting(final SurfaceViewRenderer renderer, boolean isLocalRenderer,
                               int width,int height) {
        renderer.init(rootEglBase.getEglBaseContext(), null);
        //设置大小
        final ViewGroup.LayoutParams layoutParams = renderer.getLayoutParams();
        layoutParams.width = AppConstant.SCRRENWIDTH / 2;
        layoutParams.height = (AppConstant.SCREENHEIGHT - 100) / 2;
        CallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //在UI线程更新
                renderer.setLayoutParams(layoutParams);
            }
        });

        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        if (isLocalRenderer) {
            //设置控件叠加到其他控件上面
            localRender.setZOrderMediaOverlay(true);
            renderer.setMirror(true);
        } else {
            renderer.setMirror(false);
        }

        CallActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                renderer.requestLayout();
                Log.d(TAG, "成功初始化render");
            }
        });
    }

    /**
     * 获取一个可用并且已经初始化好的Render
     *
     * @return
     */
    private SurfaceViewRenderer getAvailableRemoteRender() {
        //Log.e(TAG, "===========调用一次getAvailableRemoteRender=============");
        if (!stack_AvailableRemoteRender.empty()) {
            Log.e(TAG, "Size of Remote Render Stack is: " + stack_AvailableRemoteRender.size());
            return stack_AvailableRemoteRender.pop();
        } else {
            Log.e(TAG, "无可用的RemoteRender");
            return null;
        }
    }

    /**
     * @param instanceManager
     */
    private void allocateResources(InstanceManager instanceManager) {

        Log.d(TAG, "====================allocateResources===================");
        String roomId = instanceManager.getSigParms().roomId;


        //获取可用的render，并指定给PeerManager
        SurfaceViewRenderer viewRenderer = getAvailableRemoteRender();
        if (viewRenderer == null) {
            Log.e(TAG, "stack_AvailableRemoteRender.pop()：remoteRender is null");
            return;
        }
        instanceManager.setRemoteRenderer(viewRenderer);

        //指定WSMessageEvent
        instanceManager.setWsMessageEvent(wsMessageEvent);

        //指定WebSocketClient
        instanceManager.setWsClient(wsClient);

        //根据信令服务器传回来的iceserver参数，生成PeerConnection的配置
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(instanceManager.getSigParms().iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        MediaConstraints pcConstraints = new MediaConstraints();

        //创建并指定PCObserver
        PCObserver pcObserver = new PCObserver(instanceManager);
        instanceManager.setPcObserver(pcObserver);

        //创建并指定PeerConnection
        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig,
                pcConstraints,
                pcObserver);// 注意 pcObserver 的动作
        instanceManager.setPeerConnection(peerConnection);
        if (null == peerConnection) {
            Log.e(TAG, roomId + ":Failed to Create Peer connection.");
            return;
        }
        Log.d(TAG, roomId + ":Peer connection created.");

        //创建并指定SDPObserver
        SDPObserver sdpObserver = new SDPObserver(instanceManager);
        instanceManager.setSdpObserver(sdpObserver);

        //创建并指定MediaStream
        instanceManager.setMediaStream(vaHelper.createMediaStream(roomId));
        peerConnection.addStream(instanceManager.getMediaStream());

    }

    /**
     * 定义：先加入房间的人为master;后进入的人为slave
     *
     * @param isInitiator 是否是第一个加入房间的人
     * @param isBackup    是否由于房间满而通过backup获取的房间号
     */
    private void redefineRole(boolean isInitiator, boolean isBackup) {

        if (isBackup) {
            //由于房间满而造成的通过backup获取新的房间号，角色统一为slave
            role = AppRTC_Common.RoomRole.SLAVE;
            return;
        }

        //定义：先加入房间的人为master;后进入的人为slave
        if (isInitiator == false && role == AppRTC_Common.RoomRole.MASTER) {
            //必须在UI线程调用，否则无法显示
            CallActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "房间已被创建,您当前只能“加入该房间”", Toast.LENGTH_LONG).show();
                }
            });
            role = AppRTC_Common.RoomRole.SLAVE;
            return;
        }
        if (isInitiator == true && role == AppRTC_Common.RoomRole.SLAVE) {
            CallActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "您已经“创建房间”,等待其他人只能加入...", Toast.LENGTH_LONG).show();
                }
            });
            role = AppRTC_Common.RoomRole.MASTER;
            return;
        }
    }


    //=============================================================================

    /**
     * 向服务器发出请求。获得回应，进行处理。
     *
     * @param connectionParameters
     */
    private void joinRoom(final AppRTC_Common.RoomConnectionParameters connectionParameters) {


        String roomJoin_Url = connectionParameters.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters.roomId;
        String roomMessage = "";
        Log.d(TAG, "Connecting to room: " + roomJoin_Url + "\troomMessage:" + roomMessage);
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("POST", roomJoin_Url, null, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        Toast.makeText(CallActivity.this, "连接房间服务器失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        //开始解析返回的参数
                        final AppRTC_Common.SignalingParameters sigParms = JsonHelper.roomHttpResponseParse(response);
                        //redefineRole(sigParms.initiator, connectionParameters.backup);

                        /**
                         * Slave通过这种方式获取房间号
                         */
                        if (sigParms.result.equals("ROOM_FULL")) {

                            //加入房间失败，获取新的房间号，重新加入
                            //joinRoom(new AppRTC_Common.RoomConnectionParameters(AppRTC_Common.WebRTC_URL, sigParms.backup, true));
                            Toast.makeText(CallActivity.this, "加入房间失败:房间已满", Toast.LENGTH_SHORT).show();

                        } else if (sigParms.result.equals("SUCCESS")) {

                            clientManager = new ClientManager(MaxInstance, sigParms, connectionParameters, handler);

                            //一定要在这个“死循环线程”中运行，因为后面的连接消息推送的服务器的时候，会对此线程检查
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    wsMessageEvent = new WSMessageEvent();
                                    wsClient = new WebSocketClient(handler,
                                            wsMessageEvent,
                                            sigParms.roomId);
                                    wsClient.connect(sigParms.wssUrl, sigParms.wssPostUrl);
                                    wsClient.register(sigParms.roomId, sigParms.clientId);


                                    int roomSize = Integer.valueOf(sigParms.roomSize);
                                    boolean isPeerInitiator;
                                    for (HashMap.Entry entry : clientManager.getMap_instaces().entrySet()) {
                                        if (roomSize > 0) {
                                            isPeerInitiator = false;
                                        } else {
                                            isPeerInitiator = true;
                                        }

                                        InstanceManager instanceManager = (InstanceManager) entry.getValue();
                                        instanceManager.setPeerInitiator(isPeerInitiator);
                                        //加入房间成功，开始分配资源
                                        allocateResources(instanceManager);

                                        signalingParametersReady((InstanceManager) entry.getValue(), roomSize);

                                        roomSize--;

                                    }
                                }
                            });
                        }
                    }
                });
        httpConnection.send();
    }


    private void signalingParametersReady(final InstanceManager instanceManager, final int count) {

        //final String roomId = instanceManager.getRoomId();

        executor.execute(new Runnable() {
            @Override
            public void run() {

                if (null == instanceManager.getPeerConnection()) {
                    Log.e(TAG, roomId + ":Failed to Create Peer connection.");
                    return;
                }
                //Log.d(TAG, roomId + ":Peer connection created.");

                if (instanceManager.isPeerInitiator()) {
                    Log.d(TAG, instanceManager.getLocalInstanceId() + " Creating OFFER...");
                    // Create offer. create成功之后，会调用sdpObserver的onCreateSuccess方法
                    instanceManager.getPeerConnection().createOffer(instanceManager.getSdpObserver(), new MediaConstraints());
                } else {
                    Log.d(TAG, instanceManager.getLocalInstanceId() + " Gain Offer and Candidate...");
                    instanceManager.gainMessage(count);
                }
            }
        });

    }

    @Override
    public void onClose(SurfaceViewRenderer renderer, String roomId) {
//        Log.e(TAG, "onClose:开始回收render");
//        //PeerManager的disconnect会触发这里。回收并初始化刚刚释放的render
//        renderSetting(renderer, false);
//        stack_AvailableRemoteRender.push(renderer);
//        //map_PeerManager.remove(roomId);
    }

    private void closeOwn() {
        try {

            Log.d(TAG, "Closing websocket");
            if (wsClient != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //和信令服务器断开链接
                        wsClient.disconnect(true);
                    }
                });
            }

            Log.e(TAG, "========close Own=======");

            if (factory != null) {
                factory.stopAecDump();
            }

            Log.d(TAG, "Closing peer connection factory.");
            if (factory != null) {
                factory.dispose();
                factory = null;
            }
            factoryOpyions = null;
            Log.d(TAG, "Closing peer connection done.");
            PeerConnectionFactory.stopInternalTracingCapture();
            PeerConnectionFactory.shutdownInternalTracer();

            if (localRender != null) {
                localRender.release();
                localRender = null;
            }

            vaHelper.close();

            Log.d(TAG, "close handler...");
            handler.getLooper().quit();

            Log.d(TAG, "Finish Activity...");
            CallActivity.this.setResult(RESULT_OK);
            CallActivity.this.finish();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //==============至此，本类的主体任务已经完成==============================
    //===================================================================

    /**
     * 内部类的知识点：好处多多……自己补充
     * 内部类实现，可以直接访问外部类的变量，这样可以避免大量的参数传递
     */
    //===================================================================

    /**
     *
     */
    public class PCObserver implements PeerConnection.Observer {
        private VideoTrack remoteVideoTrack;
        private InstanceManager instanceManager;
        //private IceCandidate localcandidate;

        public PCObserver(InstanceManager instanceManager) {
            this.instanceManager = instanceManager;
        }


        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            Log.d(TAG, "SignalingState: " + newState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

            Log.d(TAG, "IceConnectionState---------" + iceConnectionState);
            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                Log.d(TAG, "localInstanceId:" + instanceManager.getLocalInstanceId() + "ICE connected");
                CallActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        instanceManager.getRemoteRenderer().requestLayout();
                    }
                });

            } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                reportError(instanceManager.getSigParms().roomId,
                        "localInstanceId:" + instanceManager.getLocalInstanceId() + "ICE disconnected.");

            } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                reportError(instanceManager.getSigParms().roomId,
                        "localInstanceId:" + instanceManager.getLocalInstanceId() + "\tICE connection failed.");
            }

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "IceConnectionReceiving changed to " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            Log.d(TAG, "IceGatheringState: " + newState);
        }


        /**
         * 在SDP信息的offer/answer流程中.完成“获取本地的IceCandidate”的任务。
         * 然后调用下面的方法
         * 创建配置了ICE服务器的PC实例，并为其添加onicecandidate事件回调
         * 当网络候选可用时，将会调用onicecandidate函数
         *
         * @param candidate
         */
        @Override
        public void onIceCandidate(final IceCandidate candidate) {

            //Log.e(TAG, "instanceId: " + instanceManager.getLocalInstanceId() + "收集到本地的candidate:\t");
            //localcandidate = candidate;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.e(TAG, "发送本地onIceCandidate");
                    //做备份
                    instanceManager.getLocalQueuedRemoteCandidates_backup().add(candidate);
                    instanceManager.sendLocalIceCandidate(candidate);
                }
            });

        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    instanceManager.sendLocalIceCandidateRemovals(candidates);

                }
            });

        }

        /**
         * This method is called when a remote peer accepts a media connection.
         * 当获取远端的视频流的时候，把视频流渲染显示出来
         *
         * @param mediaStream
         */
        @Override
        public void onAddStream(final MediaStream mediaStream) {
            Log.e(TAG, "开始显示视频流");
            if (mediaStream == null) {
                Log.e(TAG, "onAddStream: stream is null");
            }

            if (mediaStream.audioTracks.size() > 1 || mediaStream.videoTracks.size() > 1) {
                Log.e(TAG, "Weird-looking stream: " + mediaStream);
                return;
            }
            if (mediaStream.videoTracks.size() == 1) {
                //设置远方传过来的视频流
                remoteVideoTrack = mediaStream.videoTracks.get(0);
                remoteVideoTrack.setEnabled(true); // renderVideo = true;
                remoteVideoTrack.addRenderer(new VideoRenderer(instanceManager.getRemoteRenderer()));

            }

        }

        /**
         * This method is called when a communication channel between the peers has ended.
         * 停止显示远端的视频流
         *
         * @param mediaStream
         */
        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            remoteVideoTrack = null;
        }

        @Override
        public void onDataChannel(DataChannel dc) {
            Log.e(TAG, "AppRTC doesn't use data channels, but got: " + dc.label()
                    + " anyway!");
        }

        @Override
        public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
            //Log.e(TAG, "onRenegotiationNeeded");
        }


    }

    //===============================================================

    /**
     *
     */
    public class SDPObserver implements SdpObserver {

        private SessionDescription localSdp; // either offer or answer SDP

        private InstanceManager instanceManager;

        public SDPObserver(InstanceManager instanceManager) {
            this.instanceManager = instanceManager;
        }


        public SessionDescription getLocalSdp() {
            return localSdp;
        }

        /**
         * 当调用peerconnection的CreateOffer/CreateAnswer成功后，onCreateSuccess函数会被触发
         *
         * @param origSdp
         */
        @Override
        public void onCreateSuccess(SessionDescription origSdp) {
//            if (localSdp != null) {
//                Log.e(TAG, "Multiple SDP createMediaStream.");
//                return;
//            }

            String sdpDescription = origSdp.description;
            sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);//audio
            sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_VP8, false);//video
            final SessionDescription sdp = new SessionDescription(origSdp.type, sdpDescription);
            localSdp = sdp;

            Log.e(TAG, instanceManager.getLocalInstanceId() + " Success to Create SDP offer/answer!");

            handler.post(new Runnable() {
                @Override
                public void run() {
                    instanceManager.setLocalDescription(sdp);
                }
            });

        }

        /**
         * 设置好SessionDescription后要做的动作
         */
        @Override
        public void onSetSuccess() {

            if (instanceManager.getPeerConnection() == null) {
                return;
            }
            PeerConnection peerConnection_tmp = instanceManager.getPeerConnection();
            if (instanceManager.isPeerInitiator()) {
                // For offering peer connection we first createMediaStream offer and set
                // local SDP, then after receiving answer set remote SDP.
                if (peerConnection_tmp.getRemoteDescription() == null) {
                    // We've just set our local SDP so time to send it.
                    Log.d(TAG, instanceManager.getLocalInstanceId() + "/" + instanceManager.getRemoteInstanceId() + " 发起者成功设置offer sdp：Local SDP set succesfully");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //做备份
                            instanceManager.setLocalSdp_backup(localSdp);
                            instanceManager.sendOfferSdp(localSdp);
                        }
                    });
                } else {
                    // We've just set remote description, so drain remote
                    // and send local ICE candidates.
                    Log.d(TAG, instanceManager.getLocalInstanceId() + "/" + instanceManager.getRemoteInstanceId() +
                            " 发起者成功设置answer sdp：Remote SDP set succesfully");
                    drainCandidates();
                }
            } else {
                // For answering peer connection we set remote SDP and then
                // createMediaStream answer and set local SDP.
                if (peerConnection_tmp.getLocalDescription() != null) {
                    // We've just set our local SDP so time to send it, drain
                    // remote and send local ICE candidates.
                    Log.d(TAG, instanceManager.getLocalInstanceId() + "/" + instanceManager.getRemoteInstanceId() +
                            " 加入者成功设置answer sdp：Local SDP set succesfully");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //将本地的sdp发送给信令服务器，并推送给已经注册过的用户，响应“先来者”的offer
                            instanceManager.sendAnswerSdp(localSdp);
                        }
                    });
                    drainCandidates();
                } else {
                    // We've just set remote SDP
                    // answer will be created soon.
                    //因为，如果不是发起者，本地sdp没有设置，但却调用了onSetSuccess，说明是设置远端sdp引起的
                    Log.d(TAG, instanceManager.getLocalInstanceId() + "/" + instanceManager.getRemoteInstanceId() +
                            " 加入者成功设置offer sdp：Remote SDP set succesfully");

                    //createMediaStream answer SDP
                    ShowUtil.logAndToast(context, "Creating ANSWER...");
                    // Create SDP constraints.
                    MediaConstraints sdpMediaConstraints = new MediaConstraints();
                    sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                            "OfferToReceiveAudio", "true"));
                    sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                            "OfferToReceiveVideo", "true"));
                    // Create answer. 成功之后，会调用sdpObserver的onCreateSuccess方法
                    peerConnection_tmp.createAnswer(instanceManager.getSdpObserver(), sdpMediaConstraints);

                }
            }

        }

        @Override
        public void onCreateFailure(String s) {
            reportError(instanceManager.getSigParms().roomId, "createSDP error: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            reportError(instanceManager.getSigParms().roomId, "setSDP error: " + s);
        }


        public void drainCandidates() {
            LinkedList<IceCandidate> queuedRemoteCandidates = instanceManager.getQueuedRemoteCandidates();
            if (queuedRemoteCandidates != null) {
                //Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
                for (int i = 0; i < queuedRemoteCandidates.size(); i++) {
                    instanceManager.getPeerConnection().addIceCandidate(queuedRemoteCandidates.get(i));
                }
            }
            queuedRemoteCandidates.clear();
        }
    }


    /**
     *
     */
    public class WSMessageEvent implements WebSocketClient.IWebSocketMessageEvent {

        private String TAG = AppRTC_Common.TAG_COMM + "WSMessageEvent";

        public WSMessageEvent() {
        }


        /////////////////////////////////////////////////////////////
        private LinkedList<IceCandidate> queuedRemoteCandidates;


        /**
         * 收到推送过来的消息，分情况处理。这里是重点。
         * 可能收到“对方的SessionDescription”或者“对方的Candidate”
         * 对于SDP，如果是Initiator，则收到answer；如果不是Initiator，则收到offer；否则报错.
         * 对于推送过来的SessionDescription和Candidate，保存起来就好
         *
         * @param msg
         */
        @Override
        public void onWebSocketMessage(String msg) {
            //这里写了这么多的if-else，就是为了把错误处理的更加详细一点。注意逻辑，不要搞混了
            //关键的就那么几句
            try {
                JSONObject json = new JSONObject(msg);
                String msgText = json.getString("msg");
                String errorText = json.optString("error");
                if (msgText.length() > 0) {
                    json = new JSONObject(msgText);
                    String type = json.optString("type");
                    String receiverId = json.optString("receiverId");
                    String senderId = json.optString("senderId");

                    final InstanceManager instanceManager = clientManager.getInstanceManager(receiverId);
                    if (instanceManager == null) {
                        //Log.e(TAG, "收到无用信息_无对应的instanceManager——忽略:receiverId:" + receiverId);
                        return;
                    } else {
                        Log.e(TAG, "找到对应的instanceManager——receiverId:" + receiverId + "\tsenderId:" + senderId);
                    }
                    instanceManager.setRemoteInstanceId(senderId);

                    if (type.equals("candidate")) {
                        ////////////////////////////
                        final JSONObject finalJson = json;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                instanceManager.addRemoteIceCandidate(toJavaCandidate(finalJson));
                            }
                        });
                    } else if (type.equals("remove-candidates")) {
                        JSONArray candidateArray = json.getJSONArray("candidates");
                        final IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                        for (int i = 0; i < candidateArray.length(); ++i) {
                            candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                        }
                        ///////////////////
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                instanceManager.removeRemoteIceCandidates(candidates);

                            }
                        });
                    } else if (type.equals("answer")) {
                        if (instanceManager.isPeerInitiator()) {
                            final SessionDescription sdp = new SessionDescription(
                                    SessionDescription.Type.fromCanonicalForm(type),
                                    json.getString("sdp"));

                            //////////////////////////////////////////////
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    instanceManager.setRemoteDescription(sdp);
                                }
                            });

                        } else {
                            reportError(roomId, "Received answer for call initiator: " + msg);
                        }
                    } else if (type.equals("offer")) {
                        Log.e(TAG, "规定：WebSocket 不允许接收offer");
                        if (!instanceManager.isPeerInitiator()) {
                            Log.e(TAG, "==============================");
                            final SessionDescription sdp = new SessionDescription(
                                    SessionDescription.Type.fromCanonicalForm(type),
                                    json.getString("sdp"));

                            ///////////////////////////////////////////
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    instanceManager.setRemoteDescription(sdp);
                                }
                            });

                        } else {
                            reportError(roomId, "Received offer for call receiver: " + msg);
                        }
                    } else if (type.equals("bye")) {
                        Log.e(TAG, "==========recieve bye!========");

                        if (!instanceManager.isPeerInitiator()) {
                            //开始执行断开连接操作
                            instanceManager.disconnect();
                        } else {
                            //instanceManager.resendLocalSdp();
                            //instanceManager.resendLocalIceCandidate();
                        }
                        //AppRTC_Common.map_isInitiator.put(roomId, true);
                        //peerConnection.createOffer(sdpObserver, new MediaConstraints());
                        //instanceManager.disconnect();
//                        renderSetting(instanceManager.getRemoteRenderer(), false);
//                        stack_AvailableRemoteRender.push(instanceManager.getRemoteRenderer());


                    } else {
                        reportError(roomId, "Unexpected WebSocket message: " + msg);
                    }
                } else {
                    if (errorText != null && errorText.length() > 0) {
                        reportError(roomId, "WebSocket error message: " + errorText);
                    } else {
                        reportError(roomId, "Unexpected WebSocket message: " + msg);
                    }
                }
            } catch (JSONException e) {
                reportError(roomId, "WebSocket message JSON parsing error: " + e.toString());
            }

        }

        @Override
        public void onWebSocketClose() {
            //关闭WebSocket数据传输
            Log.e(TAG, "WebSocket已关闭");
        }

        @Override
        public void onWebSocketError(String description) {
            Log.e(TAG, "消息推送错误：" + description);
        }


    }


    // ==========================Helper functions============================
    public static void reportError(String roomId, final String errorMessage) {
        Log.e(TAG, "reportError: " + errorMessage);
//
//        if (AppRTC_Common.map_roomState.get(roomId) != ConnectionState.ERROR) {
//            AppRTC_Common.map_roomState.put(roomId, ConnectionState.ERROR);
//            Log.e(TAG, errorMessage);
//        }

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Toast.makeText(context, "再按一次退出视频通话", Toast.LENGTH_SHORT).show();
            if (ClickUtil.isFastDoubleClick()) {
                //主动模拟人的点击动作
                cancel_btn.performClick();
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


}