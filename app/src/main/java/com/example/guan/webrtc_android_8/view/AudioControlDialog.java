package com.example.guan.webrtc_android_8.view;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.example.guan.webrtc_android_8.R;
import com.example.guan.webrtc_android_8.common.AppConstant;

/**
 * Created by guan on 4/6/17.
 */

public class AudioControlDialog extends Dialog {

    Switch Microphone_switch;
    Switch Speaker_switch;
    Button AcceptAudio_btn;


    AudioControlDialogCallback callback;


    public AudioControlDialog(@NonNull Context context) {
        super(context);
        setCustomDialog();
        getWindow().setLayout((int) (AppConstant.SCRRENWIDTH * 0.7), 800);
    }

    public void setCallback(AudioControlDialogCallback callback)
    {
        this.callback=callback;
    }

    public void setSwitchState(boolean microphoneIsChecked,boolean speakerIsChecked)
    {
        Microphone_switch.setChecked(microphoneIsChecked);
        Speaker_switch.setChecked(speakerIsChecked);
    }

    private void setCustomDialog() {
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_audiocontrol, null);
        Microphone_switch = (Switch) mView.findViewById(R.id.Microphone_switch);
        Speaker_switch = (Switch) mView.findViewById(R.id.Speaker_switch);
        AcceptAudio_btn = (Button) mView.findViewById(R.id.AcceptAudio_btn);


        AcceptAudio_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onClickAudioSwitch(Microphone_switch.isChecked(),Speaker_switch.isChecked());
            }
        });


        //ViewGroup.LayoutParams params=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        super.setContentView(mView);
    }


    public interface AudioControlDialogCallback {
        void onClickAudioSwitch(boolean microphoneIsChecked,boolean speakerIsChecked);
    }

}
