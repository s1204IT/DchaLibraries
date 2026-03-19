package com.mediatek.server;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import java.lang.Thread;

public class MtkShutdownThread extends ShutdownThread {
    private static final int ANIMATION_MODE = 1;
    private static final int BACKLIGHT_STATE_POLL_SLEEP_MSEC = 50;
    private static final int DEFAULT_MODE = 0;
    private static final int MAX_BLIGHT_OFF_DELAY_TIME = 5000;
    private static final int MAX_BLIGHT_OFF_POLL_TIME = 1000;
    private static final int MIN_SHUTDOWN_ANIMATION_PLAY_TIME = 5000;
    private static final String OPERATOR_SYSPROP = "persist.vendor.operator.optr";
    private static final String SHUTDOWN_SYSPROP = "ro.vendor.mtk_shutvideo.enable";
    private static String TAG = "MtkShutdownThread";
    private static boolean DEBUG = true;
    private static boolean mSpew = false;
    private static boolean mBlightOff = false;
    private static long beginAnimationTime = 0;
    private static long endAnimationTime = 0;
    private static boolean bConfirmForAnimation = true;
    private static boolean bPlayaudio = true;
    private static int mShutOffAnimation = -1;
    private static Runnable mDelayDim = new Runnable() {
        @Override
        public void run() {
            if (((MtkShutdownThread) MtkShutdownThread.sInstance).mScreenWakeLock != null && ((MtkShutdownThread) MtkShutdownThread.sInstance).mScreenWakeLock.isHeld()) {
                ((MtkShutdownThread) MtkShutdownThread.sInstance).mScreenWakeLock.release();
            }
            if (((MtkShutdownThread) MtkShutdownThread.sInstance).mPowerManager == null) {
                ((MtkShutdownThread) MtkShutdownThread.sInstance).mPowerManager = (PowerManager) ((MtkShutdownThread) MtkShutdownThread.sInstance).mContext.getSystemService("power");
            }
            MtkShutdownThread.setBacklightOff();
        }
    };

    protected boolean mIsShowShutdownSysui() {
        return isCustBootAnim() != 1;
    }

    protected boolean mIsShowShutdownDialog(Context context) {
        if (showShutdownAnimation(context)) {
            return false;
        }
        return true;
    }

    protected boolean mStartShutdownSeq(Context context) {
        if (getState() != Thread.State.NEW || isAlive()) {
            Log.i(TAG, "Thread state is not normal! froce to shutdown!");
            if (isCustBootAnim() == 1) {
                delayForPlayAnimation();
            }
            setBacklightOff();
            PowerManagerService.lowLevelShutdown(mReason);
            return false;
        }
        int screenTurnOffTime = 5000;
        switchToLauncher(context);
        if (isCustBootAnim() == 1) {
            screenTurnOffTime = getScreenTurnOffTime(context);
        }
        this.mHandler.postDelayed(mDelayDim, screenTurnOffTime);
        return true;
    }

    protected void mShutdownSeqFinish(Context context) {
        shutdownAnimationService();
        setBacklightOff();
    }

    protected void mLowLevelShutdownSeq(Context context) {
        pollBacklightOff(context);
        if (mSpew && SystemProperties.getInt("vendor.shutdown_delay", DEFAULT_MODE) == 1) {
            Log.i(TAG, "Delay Shutdown 5s");
            SystemClock.sleep(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
        }
    }

    private static void setBacklightOff() {
        if (mBlightOff) {
            return;
        }
        if (((MtkShutdownThread) sInstance).mPowerManager == null) {
            Log.e(TAG, "check PowerManager: PowerManager service is null");
            return;
        }
        mBlightOff = true;
        Log.i(TAG, "setBacklightBrightness: Off");
        ((MtkShutdownThread) sInstance).mPowerManager.goToSleep(SystemClock.uptimeMillis(), 8, DEFAULT_MODE);
    }

    private void pollBacklightOff(Context context) {
        try {
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            DisplayInfo displayInfo = new DisplayInfo();
            long jElapsedRealtime = SystemClock.elapsedRealtime() + 1000;
            long jElapsedRealtime2 = jElapsedRealtime - SystemClock.elapsedRealtime();
            while (jElapsedRealtime2 > 0) {
                displayManager.getDisplay(DEFAULT_MODE).getDisplayInfo(displayInfo);
                if (displayInfo.state == 1) {
                    break;
                }
                SystemClock.sleep(50L);
                jElapsedRealtime2 = jElapsedRealtime - SystemClock.elapsedRealtime();
            }
            Log.i(TAG, "Backlight polling take:" + (1000 - jElapsedRealtime2) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchToLauncher(Context context) {
        Log.i(TAG, "set launcher as foreground");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.setFlags(268435456);
        context.startActivity(intent);
    }

    private void shutdownAnimationService() {
        if (isCustBootAnim() != 1) {
            return;
        }
        Log.i(TAG, "set service.shutanim.running to 1");
        SystemProperties.set("service.shutanim.running", "1");
        if ((mReboot && mReason != null && mReason.equals("recovery")) || !mReboot || SystemProperties.getInt(SHUTDOWN_SYSPROP, DEFAULT_MODE) == 1) {
            delayForPlayAnimation();
        }
    }

    private boolean showShutdownAnimation(Context context) {
        beginAnimationTime = 0L;
        if (isCustBootAnim() == 1) {
            configShutdownAnimation(context);
            bootanimCust(context);
            return true;
        }
        return false;
    }

    private static void bootanimCust(Context context) {
        SystemProperties.set("service.shutanim.running", "0");
        Log.i(TAG, "set service.shutanim.running to 0");
        try {
            if (Settings.System.getInt(context.getContentResolver(), "accelerometer_rotation", 1) != 0 ? true : DEFAULT_MODE) {
                IWindowManager iWindowManagerAsInterface = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                if (iWindowManagerAsInterface != null) {
                    iWindowManagerAsInterface.freezeRotation(DEFAULT_MODE);
                }
                Settings.System.putInt(context.getContentResolver(), "accelerometer_rotation", DEFAULT_MODE);
                Settings.System.putInt(context.getContentResolver(), "accelerometer_rotation_restore", 1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e2) {
            Log.e(TAG, "check Rotation: context object is null when get Rotation");
        }
        beginAnimationTime = SystemClock.elapsedRealtime() + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
        try {
            IWindowManager iWindowManagerAsInterface2 = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (iWindowManagerAsInterface2 != null) {
                iWindowManagerAsInterface2.setEventDispatching(false);
            }
        } catch (RemoteException e3) {
            e3.printStackTrace();
        }
        startBootAnimation();
    }

    private static void configShutdownAnimation(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (!bConfirmForAnimation && !powerManager.isScreenOn()) {
            bPlayaudio = false;
        } else {
            bPlayaudio = true;
        }
    }

    private static void startBootAnimation() {
        Log.i(TAG, "Set 'service.bootanim.exit' = 0).");
        SystemProperties.set("service.bootanim.exit", "0");
        if (bPlayaudio) {
            SystemProperties.set("ctl.start", "banim_shutmp3");
            Log.i(TAG, "bootanim:shut mp3");
        } else {
            SystemProperties.set("ctl.start", "banim_shutnomp3");
            Log.i(TAG, "bootanim:shut nomp3");
        }
    }

    private static void delayForPlayAnimation() {
        if (beginAnimationTime <= 0) {
            return;
        }
        endAnimationTime = beginAnimationTime - SystemClock.elapsedRealtime();
        if (endAnimationTime > 0) {
            try {
                Thread.currentThread();
                Thread.sleep(endAnimationTime);
            } catch (InterruptedException e) {
                Log.e(TAG, "Shutdown stop bootanimation Thread.currentThread().sleep exception!");
            }
        }
    }

    public static int getScreenTurnOffTime() {
        if (SystemProperties.get(OPERATOR_SYSPROP, "0").equals("OP01")) {
            Log.i(TAG, "Inside MtkShutdownThread OP01");
            return 4000;
        }
        if (SystemProperties.get(OPERATOR_SYSPROP, "0").equals("OP02")) {
            Log.i(TAG, "Inside MtkShutdownThread OP02");
            return 4000;
        }
        if (!SystemProperties.get(OPERATOR_SYSPROP, "0").equals("OP09")) {
            return 4000;
        }
        Log.i(TAG, "Inside MtkShutdownThread OP09");
        return 3000;
    }

    private static int getScreenTurnOffTime(Context context) {
        int screenTurnOffTime;
        try {
            screenTurnOffTime = getScreenTurnOffTime();
        } catch (Exception e) {
            e = e;
            screenTurnOffTime = DEFAULT_MODE;
        }
        try {
            Log.i(TAG, "screen turn off time screenTurnOffTime =" + screenTurnOffTime);
        } catch (Exception e2) {
            e = e2;
            e.printStackTrace();
        }
        return screenTurnOffTime;
    }

    public static int isCustBootAnim() {
        if (mShutOffAnimation == -1) {
            String str = SystemProperties.get(OPERATOR_SYSPROP, "0");
            int i = 1;
            if (str.equals("OP01")) {
                if (!RatConfiguration.isLteFddSupported() && !RatConfiguration.isLteTddSupported()) {
                    i = DEFAULT_MODE;
                }
            } else if (!str.equals("OP02") && !str.equals("OP09")) {
                if (SystemProperties.getInt(SHUTDOWN_SYSPROP, DEFAULT_MODE) == 1) {
                    Log.i(TAG, "Inside MtkShutdownThread shutvideo");
                } else {
                    str = "NONE";
                    i = DEFAULT_MODE;
                }
            }
            mShutOffAnimation = i;
            Log.i(TAG, "mShutOffAnimation: " + i + " (" + str + ")");
            return i;
        }
        return mShutOffAnimation;
    }
}
