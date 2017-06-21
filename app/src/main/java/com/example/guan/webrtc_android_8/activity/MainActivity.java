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
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;
import com.example.guan.webrtc_android_8.common.AppRTC_Common;
import com.example.guan.webrtc_android_8.utils.AsyncHttpURLConnection;
import com.example.guan.webrtc_android_8.utils.ClickUtil;
import com.example.guan.webrtc_android_8.utils.ShowUtil;
import com.example.guan.webrtc_android_8.view.ModeSettingDialog;
import com.example.guan.webrtc_android_8.view.ServerSettingDialog;
import com.example.guan.webrtc_android_8.view.YesOrNoDialog;

import java.util.ArrayList;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity {

    String TAG = AppRTC_Common.TAG_COMM + "MainActivity";

    ArrayList<String> permissionList = new ArrayList<String>();


    Button startCall_btn;
    Button serverSetting_btn;
    EditText roomid_et;
    TextView selectedServer_tv;
    ImageButton modeSetting_imgbtn;
    TextView selectedMode_tv;
    RadioGroup role_radiogrp;
    AppRTC_Common.RoomRole role;
    String roomid;
    Context mContext;

    Button TestNum1_btn;
    Button TestNum2_btn;
    Button TestNum3_btn;
    Button ClearRoom_btn;

    View.OnClickListener TestNum_listener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initUI();
        checkAndRequestPermissions(permissionList);
    }

    private void initData() {
        mContext = this;
        saveScreenSize();
        permissionList.add(Manifest.permission.CAMERA);
        permissionList.add(Manifest.permission.RECORD_AUDIO);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        TestNum_listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomid_et.setText(((Button) v).getHint());
                startCall_btn.performClick();
            }
        };
    }

    private void initUI() {

        Window window = this.getWindow();
        window.setStatusBarColor(Color.LTGRAY);

        startCall_btn = (Button) this.findViewById(R.id.Call_btn);
        serverSetting_btn = (Button) this.findViewById(R.id.serversetting_btn);
        roomid_et = (EditText) this.findViewById(R.id.RoomID_et);
        selectedServer_tv = (TextView) this.findViewById(R.id.serversetting_tv);
        role_radiogrp = (RadioGroup) this.findViewById(R.id.role_radiogrp);
        modeSetting_imgbtn = (ImageButton) this.findViewById(R.id.selectmode_imgbtn);
        selectedMode_tv = (TextView) this.findViewById(R.id.modesetting_tv);

        TestNum1_btn = (Button) this.findViewById(R.id.TestNum1_btn);
        TestNum2_btn = (Button) this.findViewById(R.id.TestNum2_btn);
        TestNum3_btn = (Button) this.findViewById(R.id.TestNum3_btn);

        ClearRoom_btn = (Button) this.findViewById(R.id.ClearRoom_btn);


        role_radiogrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.Master_radiobtn) {
                    role = AppRTC_Common.RoomRole.MASTER;

                } else if (checkedId == R.id.Slave_radiobtn) {
                    role = AppRTC_Common.RoomRole.SLAVE;

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

                if (AppRTC_Common.selected_WebRTC_URL.equals("") || AppRTC_Common.selected_turnServer_URL.equals("")) {
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
                AppRTC_Common.selected_roomId = roomid;
                AppRTC_Common.selected_role = role;
                Log.d(TAG, "roomurl:" + AppRTC_Common.selected_WebRTC_URL +
                        "\troomid:" + AppRTC_Common.selected_roomId +
                        "\trole:" + AppRTC_Common.selected_role +
                        "\tmode: " + AppRTC_Common.selectedMode);


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
                            AppRTC_Common.selected_WebRTC_URL = AppRTC_Common.labServer_WebRTC_URL;
                            AppRTC_Common.selected_turnServer_URL = AppRTC_Common.labServer_turnServer_URL;
                        } else if (s == ServerSettingDialog.SelectedServer.Aliyun) {
                            AppRTC_Common.selected_WebRTC_URL = AppRTC_Common.aliyun_WebRTC_URL;
                            AppRTC_Common.selected_turnServer_URL = AppRTC_Common.aliyun_turnServer_URL;
                        }

                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        selectedServer_tv.setText(AppRTC_Common.selected_WebRTC_URL);

                    }
                });
                dialog.show();
            }
        });
        selectedServer_tv.setText(AppRTC_Common.selected_WebRTC_URL);



        modeSetting_imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModeSettingDialog dialog = new ModeSettingDialog(mContext);
                dialog.setCanceledOnTouchOutside(true);//触摸dialog以外的区域，dialog会消失
                dialog.setDialogCallback(new ModeSettingDialog.DialogCallback() {
                    @Override
                    public void onClickRadioButton(ModeSettingDialog.Mode mode) {
                        if (mode == ModeSettingDialog.Mode.M_2_M) {
                            AppRTC_Common.selectedMode = AppRTC_Common.M_2_M;
                        } else if (mode == ModeSettingDialog.Mode.P_2_M) {
                            AppRTC_Common.selectedMode = AppRTC_Common.P_2_M;
                        }
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //showSelectedServerOnActivity();
                        selectedMode_tv.setText(AppRTC_Common.selectedMode);
                    }
                });
                dialog.show();
            }
        });
        selectedMode_tv.setText(AppRTC_Common.selectedMode);

        TestNum1_btn.setOnClickListener(TestNum_listener);
        TestNum2_btn.setOnClickListener(TestNum_listener);
        TestNum3_btn.setOnClickListener(TestNum_listener);


    }


    private void checkAndRequestPermissions(ArrayList<String> permissionList) {

        ArrayList<String> list = new ArrayList<>(permissionList);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String permission = it.next();
            int hasPermission = ContextCompat.checkSelfPermission(this, permission);
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                it.remove();
            }
        }

        if (list.size() == 0) {
            return;
        }
        String[] permissions = list.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissions, AppConstant.MY_PERMISSIONS_REQUEST);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AppConstant.MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                int length = grantResults.length;
                boolean re_request = false;
                for (int i = 0; i < length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "权限授予成功:" + permissions[i]);
                    } else {
                        Log.e(TAG, "权限授予失败:" + permissions[i]);
                        re_request = true;
                    }
                }
                if (re_request) {
                    //弹出对话框，提示用户重新授予权限
                    final YesOrNoDialog permissionDialog = new YesOrNoDialog(mContext);
                    permissionDialog.setCanceledOnTouchOutside(false);
                    permissionDialog.setMeesage("请授予相关权限，否则程序无法运行。\n\n点击确定，重新授予权限。\n点击取消，立即终止程序。\n");
                    permissionDialog.setCallback(new YesOrNoDialog.YesOrNoDialogCallback() {
                        @Override
                        public void onClickButton(YesOrNoDialog.ClickedButton button, String message) {
                            if (button == YesOrNoDialog.ClickedButton.POSITIVE) {
                                permissionDialog.dismiss();
                                //此处需要弹出手动修改权限的系统界面
                                checkAndRequestPermissions(permissionList);
                            } else if (button == YesOrNoDialog.ClickedButton.NEGATIVE) {
                                permissionDialog.dismiss();
                                MainActivity.this.finish();
                            }
                        }
                    });

                    permissionDialog.show();
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        //根据requestCode，resultCode判断后，再进行操作。此处省略
        roomid_et.getText().clear();
        Log.e(TAG, "MainActivity finished successfully");
    }
}
