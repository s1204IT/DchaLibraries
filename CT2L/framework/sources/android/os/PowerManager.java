package android.os;

import android.content.Context;
import android.util.Log;
import com.android.internal.R;

public final class PowerManager {
    public static final int ACQUIRE_CAUSES_WAKEUP = 268435456;
    public static final String ACTION_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED";
    public static final String ACTION_POWER_SAVE_MODE_CHANGING = "android.os.action.POWER_SAVE_MODE_CHANGING";
    public static final int BRIGHTNESS_DEFAULT = -1;
    public static final int BRIGHTNESS_OFF = 0;
    public static final int BRIGHTNESS_ON = 255;
    public static final int DOZE_WAKE_LOCK = 64;
    public static final String EXTRA_POWER_SAVE_MODE = "mode";

    @Deprecated
    public static final int FULL_WAKE_LOCK = 26;
    public static final int GO_TO_SLEEP_FLAG_NO_DOZE = 1;
    public static final int GO_TO_SLEEP_REASON_APPLICATION = 0;
    public static final int GO_TO_SLEEP_REASON_DEVICE_ADMIN = 1;
    public static final int GO_TO_SLEEP_REASON_HDMI = 5;
    public static final int GO_TO_SLEEP_REASON_LID_SWITCH = 3;
    public static final int GO_TO_SLEEP_REASON_POWER_BUTTON = 4;
    public static final int GO_TO_SLEEP_REASON_TIMEOUT = 2;
    public static final int ON_AFTER_RELEASE = 536870912;
    public static final int PARTIAL_WAKE_LOCK = 1;
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
    public static final String REBOOT_RECOVERY = "recovery";
    public static final int RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY = 1;

    @Deprecated
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 10;

    @Deprecated
    public static final int SCREEN_DIM_WAKE_LOCK = 6;
    private static final String TAG = "PowerManager";
    public static final int UNIMPORTANT_FOR_LOGGING = 1073741824;
    public static final int USER_ACTIVITY_EVENT_BUTTON = 1;
    public static final int USER_ACTIVITY_EVENT_OTHER = 0;
    public static final int USER_ACTIVITY_EVENT_TOUCH = 2;
    public static final int USER_ACTIVITY_FLAG_INDIRECT = 2;
    public static final int USER_ACTIVITY_FLAG_NO_CHANGE_LIGHTS = 1;
    public static final int WAKE_LOCK_LEVEL_MASK = 65535;
    final Context mContext;
    final Handler mHandler;
    final IPowerManager mService;

    public PowerManager(Context context, IPowerManager service, Handler handler) {
        this.mContext = context;
        this.mService = service;
        this.mHandler = handler;
    }

    public int getMinimumScreenBrightnessSetting() {
        return this.mContext.getResources().getInteger(R.integer.config_screenBrightnessSettingMinimum);
    }

    public int getMaximumScreenBrightnessSetting() {
        return this.mContext.getResources().getInteger(R.integer.config_screenBrightnessSettingMaximum);
    }

    public int getDefaultScreenBrightnessSetting() {
        return this.mContext.getResources().getInteger(R.integer.config_screenBrightnessSettingDefault);
    }

    public static boolean useTwilightAdjustmentFeature() {
        return SystemProperties.getBoolean("persist.power.usetwilightadj", false);
    }

    public WakeLock newWakeLock(int levelAndFlags, String tag) {
        validateWakeLockParameters(levelAndFlags, tag);
        return new WakeLock(levelAndFlags, tag, this.mContext.getOpPackageName());
    }

    public static void validateWakeLockParameters(int levelAndFlags, String tag) {
        switch (65535 & levelAndFlags) {
            case 1:
            case 6:
            case 10:
            case 26:
            case 32:
            case 64:
                if (tag == null) {
                    throw new IllegalArgumentException("The tag must not be null.");
                }
                return;
            default:
                throw new IllegalArgumentException("Must specify a valid wake lock level.");
        }
    }

    @Deprecated
    public void userActivity(long when, boolean noChangeLights) {
        userActivity(when, 0, noChangeLights ? 1 : 0);
    }

    public void userActivity(long when, int event, int flags) {
        try {
            this.mService.userActivity(when, event, flags);
        } catch (RemoteException e) {
        }
    }

    public void goToSleep(long time) {
        goToSleep(time, 0, 0);
    }

    public void goToSleep(long time, int reason, int flags) {
        try {
            this.mService.goToSleep(time, reason, flags);
        } catch (RemoteException e) {
        }
    }

    public void wakeUp(long time) {
        try {
            this.mService.wakeUp(time);
        } catch (RemoteException e) {
        }
    }

    public void nap(long time) {
        try {
            this.mService.nap(time);
        } catch (RemoteException e) {
        }
    }

    public void boostScreenBrightness(long time) {
        try {
            this.mService.boostScreenBrightness(time);
        } catch (RemoteException e) {
        }
    }

    public void setBacklightBrightness(int brightness) {
        try {
            this.mService.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException e) {
        }
    }

    public boolean isWakeLockLevelSupported(int level) {
        try {
            return this.mService.isWakeLockLevelSupported(level);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Deprecated
    public boolean isScreenOn() {
        return isInteractive();
    }

    public boolean isInteractive() {
        try {
            return this.mService.isInteractive();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void reboot(String reason) {
        try {
            this.mService.reboot(false, reason, true);
        } catch (RemoteException e) {
        }
    }

    public boolean isPowerSaveMode() {
        try {
            return this.mService.isPowerSaveMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setPowerSaveMode(boolean mode) {
        try {
            return this.mService.setPowerSaveMode(mode);
        } catch (RemoteException e) {
            return false;
        }
    }

    public final class WakeLock {
        private int mCount;
        private int mFlags;
        private boolean mHeld;
        private String mHistoryTag;
        private final String mPackageName;
        private String mTag;
        private final String mTraceName;
        private WorkSource mWorkSource;
        private boolean mRefCounted = true;
        private final Runnable mReleaser = new Runnable() {
            @Override
            public void run() {
                WakeLock.this.release();
            }
        };
        private final IBinder mToken = new Binder();

        WakeLock(int flags, String tag, String packageName) {
            this.mFlags = flags;
            this.mTag = tag;
            this.mPackageName = packageName;
            this.mTraceName = "WakeLock (" + this.mTag + ")";
        }

        protected void finalize() throws Throwable {
            synchronized (this.mToken) {
                if (this.mHeld) {
                    Log.wtf(PowerManager.TAG, "WakeLock finalized while still held: " + this.mTag);
                    Trace.asyncTraceEnd(Trace.TRACE_TAG_POWER, this.mTraceName, 0);
                    try {
                        PowerManager.this.mService.releaseWakeLock(this.mToken, 0);
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        public void setReferenceCounted(boolean value) {
            synchronized (this.mToken) {
                this.mRefCounted = value;
            }
        }

        public void acquire() {
            synchronized (this.mToken) {
                acquireLocked();
            }
        }

        public void acquire(long timeout) {
            synchronized (this.mToken) {
                acquireLocked();
                PowerManager.this.mHandler.postDelayed(this.mReleaser, timeout);
            }
        }

        private void acquireLocked() {
            if (this.mRefCounted) {
                int i = this.mCount;
                this.mCount = i + 1;
                if (i != 0) {
                    return;
                }
            }
            PowerManager.this.mHandler.removeCallbacks(this.mReleaser);
            Trace.asyncTraceBegin(Trace.TRACE_TAG_POWER, this.mTraceName, 0);
            try {
                PowerManager.this.mService.acquireWakeLock(this.mToken, this.mFlags, this.mTag, this.mPackageName, this.mWorkSource, this.mHistoryTag);
            } catch (RemoteException e) {
            }
            this.mHeld = true;
        }

        public void release() {
            release(0);
        }

        public void release(int flags) {
            synchronized (this.mToken) {
                if (this.mRefCounted) {
                    int i = this.mCount - 1;
                    this.mCount = i;
                    if (i == 0) {
                        PowerManager.this.mHandler.removeCallbacks(this.mReleaser);
                        if (this.mHeld) {
                            Trace.asyncTraceEnd(Trace.TRACE_TAG_POWER, this.mTraceName, 0);
                            try {
                                PowerManager.this.mService.releaseWakeLock(this.mToken, flags);
                            } catch (RemoteException e) {
                            }
                            this.mHeld = false;
                        }
                    }
                }
                if (this.mCount < 0) {
                    throw new RuntimeException("WakeLock under-locked " + this.mTag);
                }
            }
        }

        public boolean isHeld() {
            boolean z;
            synchronized (this.mToken) {
                z = this.mHeld;
            }
            return z;
        }

        public void setWorkSource(WorkSource ws) {
            boolean changed;
            synchronized (this.mToken) {
                if (ws != null) {
                    if (ws.size() == 0) {
                        ws = null;
                    }
                    if (ws != null) {
                        changed = this.mWorkSource != null;
                        this.mWorkSource = null;
                    } else if (this.mWorkSource == null) {
                        changed = true;
                        this.mWorkSource = new WorkSource(ws);
                    } else {
                        changed = this.mWorkSource.diff(ws);
                        if (changed) {
                            this.mWorkSource.set(ws);
                        }
                    }
                    if (changed && this.mHeld) {
                        try {
                            PowerManager.this.mService.updateWakeLockWorkSource(this.mToken, this.mWorkSource, this.mHistoryTag);
                        } catch (RemoteException e) {
                        }
                    }
                } else {
                    if (ws != null) {
                    }
                    if (changed) {
                        PowerManager.this.mService.updateWakeLockWorkSource(this.mToken, this.mWorkSource, this.mHistoryTag);
                    }
                }
            }
        }

        public void setTag(String tag) {
            this.mTag = tag;
        }

        public void setHistoryTag(String tag) {
            this.mHistoryTag = tag;
        }

        public void setUnimportantForLogging(boolean state) {
            if (!state) {
                this.mFlags &= -1073741825;
            } else {
                this.mFlags |= 1073741824;
            }
        }

        public String toString() {
            String str;
            synchronized (this.mToken) {
                str = "WakeLock{" + Integer.toHexString(System.identityHashCode(this)) + " held=" + this.mHeld + ", refCount=" + this.mCount + "}";
            }
            return str;
        }
    }
}
