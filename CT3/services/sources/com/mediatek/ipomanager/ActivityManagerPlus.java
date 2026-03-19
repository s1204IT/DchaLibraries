package com.mediatek.ipomanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.app.ShutdownManager;
import com.android.internal.policy.PhoneWindow;
import com.android.server.usb.UsbAudioDevice;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import java.util.ArrayList;
import java.util.List;

public final class ActivityManagerPlus {
    private static final String TAG = "ActivityManagerPlus";
    private Context mContext;
    final Handler mHandler;
    private static ActivityManagerPlus sInstance = null;
    private static View mIPOWin = null;
    final HandlerThread mHandlerThread = new HandlerThread("AMPlus", -2);
    private boolean mIPOAlarmBoot = false;
    final ArrayList<String> mBoostDownloadingAppList = new ArrayList<>();

    public static ActivityManagerPlus getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ActivityManagerPlus(context);
        }
        return sInstance;
    }

    public static ActivityManagerPlus getInstance(AMEventHookData.SystemReady systemReady) {
        return getInstance((Context) systemReady.get(AMEventHookData.SystemReady.Index.context));
    }

    public AMEventHookResult filterBroadcast(AMEventHookData.BeforeSendBroadcast beforeSendBroadcast, AMEventHookResult aMEventHookResult) {
        if (!SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
            return aMEventHookResult;
        }
        Intent intent = (Intent) beforeSendBroadcast.get(AMEventHookData.BeforeSendBroadcast.Index.intent);
        String action = intent.getAction();
        if (!"android.intent.action.BOOT_COMPLETED".equals(action) && !"android.intent.action.ACTION_SHUTDOWN".equals(action) && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            return aMEventHookResult;
        }
        if (intent.getIntExtra("_mode", 0) == 0) {
            Slog.i(TAG, "normal boot/shutdown");
            return aMEventHookResult;
        }
        if ("android.intent.action.BOOT_COMPLETED".equals(action) || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            List list = (List) beforeSendBroadcast.get(AMEventHookData.BeforeSendBroadcast.Index.filterStaticList);
            List list2 = (List) beforeSendBroadcast.get(AMEventHookData.BeforeSendBroadcast.Index.filterDynamicList);
            for (String str : ShutdownManager.sShutdownWhiteList) {
                list.add(str);
                list2.add(str);
                Slog.i(TAG, "filterBroadcast:" + str);
            }
            aMEventHookResult.addAction(AMEventHookAction.AM_FilterStaticReceiver);
            aMEventHookResult.addAction(AMEventHookAction.AM_FilterRegisteredReceiver);
        } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
            List list3 = (List) beforeSendBroadcast.get(AMEventHookData.BeforeSendBroadcast.Index.filterDynamicList);
            for (String str2 : ShutdownManager.sShutdownWhiteList) {
                list3.add(str2);
                Slog.i(TAG, "filterBroadcast:" + str2);
            }
            aMEventHookResult.addAction(AMEventHookAction.AM_FilterRegisteredReceiver);
        }
        return aMEventHookResult;
    }

    private ActivityManagerPlus(Context context) {
        Slog.i(TAG, "start ActivityManagerPlus");
        this.mContext = context;
        Slog.i(TAG, "support wl!");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        startHandler();
    }

    final void startHandler() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOST_DOWNLOADING");
        intentFilter.addAction("android.intent.action.ACTION_BOOT_IPO");
        intentFilter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        intentFilter.addAction("android.intent.action.black.mode");
        intentFilter.addAction("android.intent.action.normal.boot");
        intentFilter.setPriority(1000);
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        Slog.i(TAG, "startHandler!");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                String string;
                Slog.i(ActivityManagerPlus.TAG, "Receive: " + intent);
                final ShutdownManager shutdownManager = ShutdownManager.getInstance();
                String action = intent.getAction();
                if ("android.intent.action.BOOST_DOWNLOADING".equals(action)) {
                    Bundle extras = intent.getExtras();
                    if (extras == null || (string = extras.getString("package_name")) == null) {
                        return;
                    }
                    Boolean boolValueOf = Boolean.valueOf(extras.getBoolean("enabled", false));
                    int size = ActivityManagerPlus.this.mBoostDownloadingAppList.size();
                    int i = size - 1;
                    Boolean bool = false;
                    if (size != 0) {
                        while (i >= 0 && !ActivityManagerPlus.this.mBoostDownloadingAppList.get(i).equals(string)) {
                            i--;
                        }
                        if (i < 0) {
                            bool = false;
                        } else {
                            bool = true;
                        }
                    }
                    if (boolValueOf.booleanValue() && !bool.booleanValue()) {
                        ActivityManagerPlus.this.mBoostDownloadingAppList.add(string);
                        return;
                    } else {
                        if (!boolValueOf.booleanValue() && bool.booleanValue()) {
                            ActivityManagerPlus.this.mBoostDownloadingAppList.remove(i);
                            return;
                        }
                        return;
                    }
                }
                if ("android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                    Slog.i(ActivityManagerPlus.TAG, "ipo PREBOOT_IPO");
                    Slog.i(ActivityManagerPlus.TAG, "re-launch launcher");
                    Intent intent2 = new Intent("android.intent.action.MAIN");
                    intent2.addCategory("android.intent.category.HOME");
                    intent2.setFlags(intent2.getFlags() | 268435456);
                    context.startActivity(intent2);
                    if (ShutdownManager.prebootKillProcessListSize() != 0) {
                        ActivityManagerPlus.this.mHandler.postDelayed(new Runnable() {
                            static final String C2K_PROPERTY = "ro.boot.opt_c2k_support";
                            static final String DUALTALK_PROPERTY = "persist.radio.multisim.config";
                            private static final long MAX_RADIO_ON_TIME = 180000;
                            static final String RADIOOFF2_PROPERTY = "ril.ipo.radiooff.2";
                            static final String RADIOOFF_PROPERTY = "ril.ipo.radiooff";
                            final boolean isDualTalkMode;

                            {
                                this.isDualTalkMode = "dsda".equals(SystemProperties.get(DUALTALK_PROPERTY)) || "2".equals(SystemProperties.get(C2K_PROPERTY));
                            }

                            private void waitRadioOn() {
                                Slog.i(ActivityManagerPlus.TAG, "waiting for radio on");
                                long j = 0;
                                do {
                                    boolean z = SystemProperties.getInt(RADIOOFF_PROPERTY, 1) == 0;
                                    boolean z2 = SystemProperties.getInt(RADIOOFF2_PROPERTY, 1) == 0;
                                    Slog.i(ActivityManagerPlus.TAG, "DualTalkMode=" + this.isDualTalkMode + " radioOn=" + z + " radioOn=" + z2);
                                    if ((!this.isDualTalkMode && z) || (this.isDualTalkMode && z && z2)) {
                                        Slog.i(ActivityManagerPlus.TAG, "radio on for " + (100 * j) + "ms");
                                        break;
                                    } else {
                                        try {
                                            Thread.sleep(100L);
                                            j++;
                                            Slog.i(ActivityManagerPlus.TAG, " wait radio on for " + (100 * j) + "ms");
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                } while (100 * j < MAX_RADIO_ON_TIME);
                                if (!(j * 100 < MAX_RADIO_ON_TIME)) {
                                    Slog.i(ActivityManagerPlus.TAG, "timeout to wait radio on");
                                }
                            }

                            @Override
                            public void run() {
                                if (!ActivityManagerPlus.this.isWifiOnlyDevice()) {
                                    waitRadioOn();
                                } else {
                                    Slog.i(ActivityManagerPlus.TAG, "wifi-only device, skip waiting for radio on");
                                }
                                shutdownManager.prebootKillProcess(context);
                            }
                        }, 500L);
                    } else {
                        Slog.i(ActivityManagerPlus.TAG, "prebootKillProcess list empty, don't need to perform kill");
                    }
                    Slog.i(ActivityManagerPlus.TAG, "finished");
                    if (ActivityManagerPlus.isAlarmBoot()) {
                        ActivityManagerPlus.this.mIPOAlarmBoot = true;
                        return;
                    }
                    return;
                }
                if ("android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                    Slog.i(ActivityManagerPlus.TAG, "ipo BOOT_IPO");
                    ActivityManagerPlusConnection.getInstance(context).stopSocketServer();
                    if (!ActivityManagerPlus.isAlarmBoot()) {
                        shutdownManager.restoreStates(context);
                        ActivityManagerPlus.removeIPOWin();
                        Slog.i(ActivityManagerPlus.TAG, "PMS wakeup");
                        ((PowerManager) context.getSystemService("power")).wakeUp(SystemClock.uptimeMillis());
                        ActivityManagerPlus.ipoBootCompleted();
                        return;
                    }
                    return;
                }
                if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                    Slog.i(ActivityManagerPlus.TAG, "handling SHUTDOWN_IPO finished");
                    ActivityManagerPlusConnection.getInstance(context).startSocketServer();
                    return;
                }
                if ("android.intent.action.black.mode".equals(action)) {
                    if (intent.getBooleanExtra("_black_mode", false)) {
                        ActivityManagerPlus.createIPOWin();
                    }
                } else {
                    if (!"android.media.RINGER_MODE_CHANGED".equals(action)) {
                        if (ActivityManagerPlus.this.mIPOAlarmBoot && "android.intent.action.normal.boot".equals(action)) {
                            shutdownManager.restoreStates(context);
                            ActivityManagerPlus.this.mIPOAlarmBoot = false;
                            return;
                        }
                        return;
                    }
                    SystemProperties.set("persist.sys.mute.state", Integer.toString(intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1)));
                }
            }
        }, intentFilter);
    }

    private boolean isWifiOnlyDevice() {
        return !((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0);
    }

    public static void createIPOWin() {
        Slog.i(TAG, "createIPOWin");
        if (sInstance == null || sInstance.mContext == null) {
            Slog.v(TAG, "ActivityManagerPlus not ready");
            return;
        }
        if (mIPOWin != null) {
            Slog.v(TAG, "IPOWin already exist");
            return;
        }
        PhoneWindow phoneWindow = new PhoneWindow(sInstance.mContext);
        phoneWindow.setType(2037);
        phoneWindow.setFlags(1024, 1024);
        phoneWindow.setLayout(-1, -1);
        phoneWindow.requestFeature(1);
        WindowManager.LayoutParams attributes = phoneWindow.getAttributes();
        attributes.setTitle("IPOWindow");
        attributes.flags = 1048;
        WindowManager windowManager = (WindowManager) sInstance.mContext.getSystemService("window");
        mIPOWin = phoneWindow.getDecorView();
        mIPOWin.setSystemUiVisibility(512);
        mIPOWin.setBackgroundColor(UsbAudioDevice.kAudioDeviceMetaMask);
        windowManager.addView(mIPOWin, attributes);
        SystemProperties.set("sys.ipowin.done", "1");
    }

    public static void removeIPOWin() {
        Slog.i(TAG, "removeIPOWin");
        if (sInstance == null || sInstance.mContext == null) {
            Slog.v(TAG, "ActivityManagerPlus not ready");
        } else {
            if (mIPOWin == null) {
                Slog.i(TAG, "already removed, skip!");
                return;
            }
            ((WindowManager) sInstance.mContext.getSystemService("window")).removeView(mIPOWin);
            mIPOWin = null;
            SystemProperties.set("sys.ipowin.done", "0");
        }
    }

    static UserManager getUserManager(Context context) {
        if (sInstance == null || sInstance.mContext == null) {
            Log.e(TAG, "ActivityManagerPlus not ready");
            return null;
        }
        return (UserManager) context.getSystemService("user");
    }

    public static void ipoBootCompleted() {
        if (sInstance == null || sInstance.mContext == null) {
            Log.e(TAG, "ActivityManagerPlus not ready");
            return;
        }
        UserManager userManager = getUserManager(sInstance.mContext);
        if (userManager == null) {
            Log.e(TAG, "ActivityManagerPlus not ready");
            return;
        }
        List users = userManager.getUsers();
        Intent intent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED", (Uri) null);
        intent.addFlags(150994960);
        intent.putExtra("_mode", 1);
        for (int i = 0; i < users.size(); i++) {
            int i2 = ((UserInfo) users.get(i)).id;
            intent.putExtra("android.intent.extra.user_handle", i2);
            sInstance.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(i2), "android.permission.RECEIVE_BOOT_COMPLETED", -1, null, null, null, 0, null, null);
        }
        Intent intent2 = new Intent("android.intent.action.BOOT_COMPLETED", (Uri) null);
        intent2.addFlags(150994960);
        intent2.putExtra("_mode", 1);
        for (int i3 = 0; i3 < users.size(); i3++) {
            int i4 = ((UserInfo) users.get(i3)).id;
            intent2.putExtra("android.intent.extra.user_handle", i4);
            sInstance.mContext.sendOrderedBroadcastAsUser(intent2, new UserHandle(i4), "android.permission.RECEIVE_BOOT_COMPLETED", -1, null, null, null, 0, null, null);
        }
    }

    private static boolean isAlarmBoot() {
        String str = SystemProperties.get("sys.boot.reason");
        return str != null && str.equals("1");
    }
}
