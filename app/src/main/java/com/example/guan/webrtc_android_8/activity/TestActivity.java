package com.example.guan.webrtc_android_8.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;
import com.example.guan.webrtc_android_8.common.AppRTC_Common;
import com.example.guan.webrtc_android_8.view.YesOrNoDialog;

import java.util.ArrayList;
import java.util.Iterator;

public class TestActivity extends AppCompatActivity {

    ArrayList<String> permissionList = new ArrayList<String>();
    String TAG = AppRTC_Common.TAG_COMM + "TestActivity";
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mContext = TestActivity.this;
        saveScreenSize();

        permissionList.add(Manifest.permission.CAMERA);
        permissionList.add(Manifest.permission.RECORD_AUDIO);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        checkAndRequestPermissions(permissionList);

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
                                TestActivity.this.finish();
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
}
