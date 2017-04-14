package com.example.guan.webrtc_android_8.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioGroup;


import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;


/**
 * Created by guan on 3/13/17.
 */

public class ServerSettingDialog extends Dialog {

    RadioGroup selectserver_radiogrp;
    DialogCallback callback;

    public enum SelectedServer {Lab, Aliyun}


    public ServerSettingDialog(Context context) {
        super(context, R.style.Dialog_Dim);
        setCustomeDialog();

        //设置dialog的宽度
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (int) (AppConstant.SCRRENWIDTH * 0.8); //设置宽度
        getWindow().setAttributes(lp);
        //getWindow().setLayout((int) (AppConstant.SCRRENWIDTH * 0.8), (int) (AppConstant.SCREENHEIGHT * 0.3));
    }


    public void setDialogCallback(DialogCallback callback) {
        this.callback = callback;
    }

    private void setCustomeDialog() {
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_serversetting, null);
        selectserver_radiogrp = (RadioGroup) mView.findViewById(R.id.selectserver_radiogrp);


        selectserver_radiogrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId==R.id.labserver_radiobtn)
                {
                    callback.onClickRadioButton(SelectedServer.Lab);
                }else if (checkedId==R.id.aliyunserver_radiobtn)
                {
                    callback.onClickRadioButton(SelectedServer.Aliyun);
                }

//                switch (checkedId) {
//                    case R.id.labserver_radiobtn:
//                        callback.onClickRadioButton(SelectedServer.Lab);
//                        break;
//                    case R.id.aliyunserver_radiobtn:
//                        callback.onClickRadioButton(SelectedServer.Aliyun);
//                        break;
//                }
                ServerSettingDialog.this.dismiss();
            }
        });

        super.setContentView(mView);

    }

    public interface DialogCallback {

        void onClickRadioButton(SelectedServer s);

    }

}
