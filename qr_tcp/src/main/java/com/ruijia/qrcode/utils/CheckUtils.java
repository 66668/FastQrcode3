package com.ruijia.qrcode.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CheckUtils {

    /**
     * 高版本不支持了
     * service是否运行
     *
     * @return
     */
    public static boolean isServiceAlive(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> list = (ArrayList<ActivityManager.RunningServiceInfo>) manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info : list) {
            if (info.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 高版本不支持了
     * <p>
     * 检测activity是否在栈顶（不推荐使用）
     * （但是google明确说明，这个代码不能用于核心判断，后个人测试，这个方法不好用,只能判断是否在栈顶，不会判断是否在前台显示）
     * <p>
     * This should never be used for core logic in an application, such as deciding between different behaviors based on
     * the information found here.Such uses are not supported, and will likely break in the future. For example,
     * if multiple applications can be actively running at the same time,
     * assumptions made about the meaning of the data here for purposes of control flow will be incorrect.
     *
     * @return
     */
    public static boolean isActTaskTop(Context context, String ActName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = manager.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName componentName = list.get(0).topActivity;
            if (componentName.getClassName().equals(ActName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断某个activity是否在前台显示/正在屏幕显示/act是否在在栈顶（推荐使用）
     *
     * @param context
     * @param ActName
     * @return
     */
    public static boolean isActFrontShow(Context context, String ActName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = manager.getRunningAppProcesses();
        if (list != null && list.size() >= 0) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                Log.d("SJY", "info.importance==100:" + ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND + "--info.processName=" + info.processName);
                if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                        info.processName.equals(ActName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断某个进程是否在运行
     * <p>
     * act上调用，int pid = android.os.Process.myPid();
     *
     * @param context
     * @param appInfo 或者用pid,比较info.pid是否相同
     * @return
     */
    public static boolean isProgressRunning(Context context, String appInfo) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = manager.getRunningAppProcesses();
        if (list != null && list.size() > 0) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (info.toString().equals(appInfo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断某个进程是否在运行
     * <p>
     * act上调用，int pid = android.os.Process.myPid();
     *
     * @param context
     * @param pid
     * @return
     */
    public static boolean isProgressRunning(Context context, int pid) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = manager.getRunningAppProcesses();
        if (list != null && list.size() > 0) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (info.pid == pid) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 高版本不支持了
     * <p>
     * 判断一个Activity是否正在运行
     *
     * @param context
     * @param packageName
     * @param cls
     * @return
     */
    public static boolean isActivityAlive(Context context, String packageName, String cls) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = manager.getRunningTasks(1);
        ActivityManager.RunningTaskInfo task = list.get(0);
        if (task != null) {
            return TextUtils.equals(task.topActivity.getPackageName(), packageName) &&
                    (task.topActivity.getShortClassName().contains(cls));
        }

        return false;
    }

}
