package com.mediatek.keyguard.PowerOffAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.plugins.R;

public class PowerOffAlarmManager {
    private static PowerOffAlarmManager sInstance;
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private ViewMediatorCallback mViewMediatorCallback;
    public static String sDEFAULT_TITLE = "";
    private static String sALRAM_TITLE = sDEFAULT_TITLE;
    private boolean mSystemReady = false;
    private boolean mNeedToShowAlarmView = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("mediatek.intent.action.LAUNCH_POWEROFF_ALARM".equals(action)) {
                Log.d("PowerOffAlarmManager", "LAUNCH_PWROFF_ALARM: " + action);
                PowerOffAlarmManager.this.mHandler.sendEmptyMessageDelayed(R.styleable.AppCompatTheme_windowFixedHeightMinor, 1500L);
                return;
            }
            if ("android.intent.action.normal.boot".equals(action)) {
                Log.d("PowerOffAlarmManager", "NORMAL_BOOT_ACTION: " + action);
                PowerOffAlarmManager.this.mHandler.sendEmptyMessageDelayed(R.styleable.AppCompatTheme_windowFixedWidthMajor, 2500L);
                return;
            }
            if ("android.intent.action.normal.shutdown".equals(action)) {
                Log.w("PowerOffAlarmManager", "ACTION_SHUTDOWN: " + action);
                PowerOffAlarmManager.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PowerOffAlarmManager.this.mViewMediatorCallback.hideLocked();
                    }
                }, 1500L);
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        private String getMessageString(Message message) {
            switch (message.what) {
                case R.styleable.AppCompatTheme_windowFixedHeightMinor:
                    return "ALARM_BOOT";
                case R.styleable.AppCompatTheme_windowFixedWidthMajor:
                    return "RESHOW_KEYGUARD_LOCK";
                default:
                    return null;
            }
        }

        @Override
        public void handleMessage(Message message) {
            Log.d("PowerOffAlarmManager", "handleMessage enter msg name=" + getMessageString(message));
            switch (message.what) {
                case R.styleable.AppCompatTheme_windowFixedHeightMinor:
                    PowerOffAlarmManager.this.handleAlarmBoot();
                    break;
                case R.styleable.AppCompatTheme_windowFixedWidthMajor:
                    PowerOffAlarmManager.this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                    PowerOffAlarmManager.this.mViewMediatorCallback.hideLocked();
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!PowerOffAlarmManager.this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) || PowerOffAlarmManager.this.mViewMediatorCallback.isSecure()) {
                                PowerOffAlarmManager.this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                                PowerOffAlarmManager.this.mViewMediatorCallback.showLocked(null);
                            }
                        }
                    }, 2000L);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            PowerOffAlarmManager.this.mContext.sendBroadcast(new Intent("android.intent.action.normal.boot.done"));
                        }
                    }, 4000L);
                    break;
            }
            Log.d("PowerOffAlarmManager", "handleMessage exit msg name=" + getMessageString(message));
        }
    };
    private Runnable mSendRemoveIPOWinBroadcastRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("PowerOffAlarmManager", "sendRemoveIPOWinBroadcast ... ");
            PowerOffAlarmManager.this.mContext.sendBroadcast(new Intent("alarm.boot.remove.ipowin"));
        }
    };

    public PowerOffAlarmManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.normal.shutdown");
        intentFilter.addAction("mediatek.intent.action.LAUNCH_POWEROFF_ALARM");
        intentFilter.addAction("android.intent.action.normal.boot");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public static PowerOffAlarmManager getInstance(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        if (sInstance == null) {
            sInstance = new PowerOffAlarmManager(context, viewMediatorCallback, lockPatternUtils);
        }
        return sInstance;
    }

    public void handleAlarmBoot() {
        Log.d("PowerOffAlarmManager", "handleAlarmBoot");
        this.mNeedToShowAlarmView = true;
        maybeShowAlarmView();
    }

    public void startAlarm() {
        startAlarmService();
        this.mHandler.postDelayed(this.mSendRemoveIPOWinBroadcastRunnable, 1500L);
    }

    private void startAlarmService() {
        Intent intent = new Intent("com.android.deskclock.START_ALARM");
        intent.putExtra("isAlarmBoot", true);
        intent.setPackage("com.android.deskclock");
        this.mContext.startService(intent);
    }

    public static boolean isAlarmBoot() {
        return false;
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        maybeShowAlarmView();
    }

    private void maybeShowAlarmView() {
        if (this.mSystemReady && this.mNeedToShowAlarmView) {
            this.mNeedToShowAlarmView = false;
            Log.d("PowerOffAlarmManager", "maybeShowAlarmView start to showLocked");
            if (this.mViewMediatorCallback.isShowing()) {
                this.mViewMediatorCallback.setSuppressPlaySoundFlag();
                this.mViewMediatorCallback.hideLocked();
            }
            this.mViewMediatorCallback.showLocked(null);
        }
    }

    public static void setAlarmTitle(String str) {
        sALRAM_TITLE = str;
    }

    public static String getAlarmTitle() {
        return sALRAM_TITLE;
    }
}
