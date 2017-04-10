package com.example.guan.webrtc_android_8.utils;

import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import static com.example.guan.webrtc_android_8.common.JsonHelper.jsonPut;

/**
 * Created by guan on 4/10/17.
 */

public class TestOkHttp {

    public TestOkHttp() {
        uploadTest();
    }

    public void uploadTest() {
        String FilePath = "/home/guan/test.txt";
        String url = "www.balabala.com/query";
        File file = new File(FilePath);
        HashMap<String, String> params = new HashMap<>();
        OkHttpManager.upload(
                url,
                new File[]{file}, new String[]{"file"},
                params,
                new OkHttpManager.ProgressListener() {  //在这里监听上传进度
                    @Override
                    public void onProgress(long totalSize, long currSize, boolean done, int id) {
                        //id:由于一次可以顺序上传多个文件,id表示本文件在这一批文件当中的编号
                        System.out.println("id:" + id + "\tOkHttpManager.upload.onProgress==" + currSize);
                        System.out.println("id:" + id + "\tOkHttpManager.upload.onProgress==" + totalSize);
                        if (done) {
                            System.out.println("OkHttpManager.upload.onProgress=done=" + done);
                        }
                    }
                },
                new OkHttpManager.ResultCallback() {    //在这里显示回调信息
                    @Override
                    public void onCallBack(OkHttpManager.State state, String result) {
//                        if (state == OkHttpManager.State.SUCCESS) {
//
//                            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
//
//                        } else if (state == OkHttpManager.State.FAILURE) {
//
//                            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
//
//                        } else if (state == OkHttpManager.State.NETWORK_FAILURE) {
//
//                            Toast.makeText(mContext, "网络连接超时，请检查网络连接。", Toast.LENGTH_SHORT).show();
//                        }
                    }
                });
    }

    public void downloadTest() {
        String url = "www.bababa.com/download";
        String filePath = "";
        String fileName = "";

        OkHttpManager.download(url, filePath, fileName,
                new OkHttpManager.ProgressListener() {
                    @Override
                    public void onProgress(long totalSize, long currSize, boolean done, int id) {
                        //id:由于只能但文件下载，所以id始终为0
                        System.out.println("id:" + id + "\tOkHttpManager.download.onProgress==" + currSize);
                        System.out.println("id:" + id + "\tOkHttpManager.download.onProgress==" + totalSize);
                        if (done) {
                            System.out.println("OkHttpManager.download.onProgress=done=" + done);
                        }
                    }
                },
                new OkHttpManager.ResultCallback() {
                    @Override
                    public void onCallBack(OkHttpManager.State state, String result) {

                    }
                });
    }

    public void postTest()
    {
        String url="";
        HashMap<String,String> params=new HashMap<>();
        OkHttpManager.CommonPostAsyn(url, params,
                new OkHttpManager.ResultCallback() {
                    @Override
                    public void onCallBack(OkHttpManager.State state, String result) {

                    }
                });
    }

    public void getTest()
    {
        String url="";
        //若无参数，可指定为null
        HashMap<String,String> params=null;
        OkHttpManager.CommonGetAsyn(url, params,
                new OkHttpManager.ResultCallback() {
                    @Override
                    public void onCallBack(OkHttpManager.State state, String result) {

                    }
                });
    }

    public void postJsonTest()
    {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "test");
        jsonPut(json, "id", "123456");

        String url="";

        OkHttpManager.CommonPostJson(url, json.toString(),
                new OkHttpManager.ResultCallback() {
                    @Override
                    public void onCallBack(OkHttpManager.State state, String result) {

                    }
                });

    }

}
