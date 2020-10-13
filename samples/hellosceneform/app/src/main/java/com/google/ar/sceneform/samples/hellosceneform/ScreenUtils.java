package com.google.ar.sceneform.samples.hellosceneform;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;


public class ScreenUtils {
    public static int getScreenHeight(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    /**
     * px转dp
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dp(Context context, float pxValue){
        float density = context.getResources().getDisplayMetrics().density;//得到设备的密度
        return (int) (pxValue/density+0.5f);
    }

    /**
     * dp转px
     * @param context
     * @param dpValue
     * @return
     */
    public static int dp2px(Context context, float dpValue){
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue*density+0.5f);
    }
}

