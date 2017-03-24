package com.example.guan.webrtc_android_8.utils;

/**
 * Created by guan on 3/14/17.
 */

public class ClickUtil {
    private static long lastClickTime;

    /**
     * 防止多次点击，即点击频率过快
     * @return
     */
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }
}
