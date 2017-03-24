package com.example.guan.webrtc_android_8.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;
import com.example.guan.webrtc_android_8.common.AppRTC_Common;
import com.example.guan.webrtc_android_8.utils.ClickUtil;
import com.example.guan.webrtc_android_8.view.ServerSettingDialog;
import com.example.guan.webrtc_android_8.view.YesOrNoDialog;


public class MainActivity extends AppCompatActivity {

    String TAG = AppRTC_Common.TAG_COMM + "MainActivity";


    Button startCall_btn;
    Button serverSetting_btn;
    EditText roomid_et;
    TextView selectedServer_tv;
    //RadioButton selected_rbut;
    RadioGroup role_radiogrp;
    AppRTC_Common.RoomRole role;
    String roomid;
    Context mContext;

    YesOrNoDialog ynDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initUI();
        initPermission();
    }

    private void initData() {
        mContext = this;
        saveScreenSize();
    }

    private void initUI() {

        Window window = this.getWindow();
        window.setStatusBarColor(Color.LTGRAY);

        startCall_btn = (Button) this.findViewById(R.id.Call_btn);
        serverSetting_btn = (Button) this.findViewById(R.id.serversetting_btn);
        roomid_et = (EditText) this.findViewById(R.id.RoomID_et);
        selectedServer_tv = (TextView) this.findViewById(R.id.selectserver_tv);
        role_radiogrp = (RadioGroup) this.findViewById(R.id.role_radiogrp);


        role_radiogrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId) {
                    case R.id.Master_radiobtn:
                        role = AppRTC_Common.RoomRole.MASTER;
                        break;
                    case R.id.Slave_radiobtn:
                        role = AppRTC_Common.RoomRole.SLAVE;
                        break;
                }
            }
        });

        startCall_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClickUtil.isFastDoubleClick()) {
                    return;
                }

                roomid = roomid_et.getText().toString().trim();

                if (AppRTC_Common.WebRTC_URL.equals("") || AppRTC_Common.turnServer_URL.equals("")) {
                    Toast.makeText(mContext, "请选择服务器", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (null == role || role.equals("")) {
                    Toast.makeText(mContext, "请选择角色", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (null == roomid || roomid.equals("")) {
                    Toast.makeText(mContext, "请输入房间号", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, CallActivity.class);
                intent.putExtra("roomurl", AppRTC_Common.WebRTC_URL);
                intent.putExtra("roomid", roomid);
                intent.putExtra("role", role);
                Log.d(TAG, "roomurl:" + AppRTC_Common.WebRTC_URL + "\troomid:" + roomid + "\trole:" + role);


                //startActivity(intent);
                startActivityForResult(intent, AppConstant.MAINACTIVITY_REQUEST_CODE);
            }
        });

        serverSetting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerSettingDialog dialog = new ServerSettingDialog(mContext);
                dialog.setCanceledOnTouchOutside(true);//触摸dialog以外的区域，dialog会消失
                dialog.setDialogCallback(new ServerSettingDialog.DialogCallback() {
                    @Override
                    public void onClickRadioButton(ServerSettingDialog.SelectedServer s) {
                        if (s == ServerSettingDialog.SelectedServer.Lab) {
                            AppRTC_Common.WebRTC_URL = AppRTC_Common.labServer_WebRTC_URL;
                            AppRTC_Common.turnServer_URL = AppRTC_Common.labServer_turnServer_URL;
                        } else if (s == ServerSettingDialog.SelectedServer.Aliyun) {
                            AppRTC_Common.WebRTC_URL = AppRTC_Common.aliyun_WebRTC_URL;
                            AppRTC_Common.turnServer_URL = AppRTC_Common.aliyun_turnServer_URL;
                        }

                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showSelectedServerOnActivity();
                    }
                });
                dialog.show();
            }
        });
        showSelectedServerOnActivity();


    }

    private void initPermission() {
        //Can reqeust only one set of permissions at a time

        int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasRecordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int hasStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        ynDialog = new YesOrNoDialog(mContext);
        ynDialog.setMeesage("请授予相关权限，否则程序无法运行。\n\n点击确定，重新授予权限。\n点击取消，立即终止程序。\n");
        ynDialog.setCallback(new YesOrNoDialog.YesOrNoDialogCallback() {
            @Override
            public void onClickButton(YesOrNoDialog.ClickedButton button, String message) {
                if (button == YesOrNoDialog.ClickedButton.POSITIVE) {
                    ynDialog.dismiss();
                    //此处需要弹出手动修改权限的系统界面
                    MainActivity.this.finish();
                } else if (button == YesOrNoDialog.ClickedButton.NEGATIVE) {
                    ynDialog.dismiss();
                    MainActivity.this.finish();
                }
            }
        });

        if (hasCameraPermission != PackageManager.PERMISSION_GRANTED ||
                hasRecordAudioPermission != PackageManager.PERMISSION_GRANTED ||
                hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //对于原生Android，如果用户选择了“不再提示”，那么shouldShowRequestPermissionRationale就会为true。
                //此时，用户可以弹出一个对话框，向用户解释为什么需要这项权限。
                //对于一些深度定制的系统，如果用户选择了“不再提示”，那么shouldShowRequestPermissionRationale永远为false
                ynDialog.show();
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        AppConstant.MY_PERMISSIONS_REQUEST);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }

    private void saveScreenSize() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        int height = metric.heightPixels;   // 屏幕高度（像素）
        //if(!ScreenUtil.islandscape(LoginActivity.this))
        AppConstant.SCRRENWIDTH = width;
        AppConstant.SCREENHEIGHT = height;
        Log.d(TAG, "SCRRENWIDTH:\t" + width + "\tSCREENHEIGHT:\t" + height);
    }

    private void showSelectedServerOnActivity() {
        selectedServer_tv.setText(AppRTC_Common.WebRTC_URL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AppConstant.MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                int length = grantResults.length;
                for (int i = 0; i < length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "权限授予成功:" + permissions[i]);
                    } else {
                        Log.e(TAG, "权限授予失败:" + permissions[i]);
                        ynDialog.show();
                    }
                }
                break;
            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        //根据requestCode，resultCode判断后，再进行操作。此处省略
        roomid_et.getText().clear();
    }
}
