package com.android.server;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IVibratorService;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class VibratorService extends IVibratorService.Stub implements InputManager.InputDeviceListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "VibratorService";
    private final IAppOpsService mAppOpsService;
    private final IBatteryStats mBatteryStatsService;
    private final Context mContext;
    private Vibration mCurrentVibration;
    private InputManager mIm;
    private boolean mInputDeviceListenerRegistered;
    private boolean mLowPowerMode;
    private PowerManagerInternal mPowerManagerInternal;
    private SettingsObserver mSettingObserver;
    volatile VibrateThread mThread;
    private boolean mVibrateInputDevicesSetting;
    private final LinkedList<Vibration> mVibrations;
    private final PowerManager.WakeLock mWakeLock;
    private final WorkSource mTmpWorkSource = new WorkSource();
    private final Handler mH = new Handler();
    private final ArrayList<Vibrator> mInputDeviceVibrators = new ArrayList<>();
    private int mCurVibUid = -1;
    private final Runnable mVibrationRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (VibratorService.this.mVibrations) {
                VibratorService.this.doCancelVibrateLocked();
                VibratorService.this.startNextVibrationLocked();
            }
        }
    };
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                synchronized (VibratorService.this.mVibrations) {
                    if (VibratorService.this.mCurrentVibration != null && !VibratorService.this.mCurrentVibration.isSystemHapticFeedback()) {
                        VibratorService.this.doCancelVibrateLocked();
                    }
                    Iterator<Vibration> it = VibratorService.this.mVibrations.iterator();
                    while (it.hasNext()) {
                        Vibration vibration = it.next();
                        if (vibration != VibratorService.this.mCurrentVibration) {
                            VibratorService.this.unlinkVibration(vibration);
                            it.remove();
                        }
                    }
                }
            }
        }
    };

    static native boolean vibratorExists();

    static native void vibratorOff();

    static native void vibratorOn(long j);

    private class Vibration implements IBinder.DeathRecipient {
        private final String mOpPkg;
        private final long[] mPattern;
        private final int mRepeat;
        private final long mStartTime;
        private final long mTimeout;
        private final IBinder mToken;
        private final int mUid;
        private final int mUsageHint;

        Vibration(VibratorService vibratorService, IBinder token, long millis, int usageHint, int uid, String opPkg) {
            this(token, millis, null, 0, usageHint, uid, opPkg);
        }

        Vibration(VibratorService vibratorService, IBinder token, long[] pattern, int repeat, int usageHint, int uid, String opPkg) {
            this(token, 0L, pattern, repeat, usageHint, uid, opPkg);
        }

        private Vibration(IBinder token, long millis, long[] pattern, int repeat, int usageHint, int uid, String opPkg) {
            this.mToken = token;
            this.mTimeout = millis;
            this.mStartTime = SystemClock.uptimeMillis();
            this.mPattern = pattern;
            this.mRepeat = repeat;
            this.mUsageHint = usageHint;
            this.mUid = uid;
            this.mOpPkg = opPkg;
        }

        @Override
        public void binderDied() {
            synchronized (VibratorService.this.mVibrations) {
                VibratorService.this.mVibrations.remove(this);
                if (this == VibratorService.this.mCurrentVibration) {
                    VibratorService.this.doCancelVibrateLocked();
                    VibratorService.this.startNextVibrationLocked();
                }
            }
        }

        public boolean hasLongerTimeout(long millis) {
            if (this.mTimeout != 0 && this.mStartTime + this.mTimeout >= SystemClock.uptimeMillis() + millis) {
                return true;
            }
            return VibratorService.DEBUG;
        }

        public boolean isSystemHapticFeedback() {
            if ((this.mUid == 1000 || this.mUid == 0) && this.mRepeat < 0) {
                return true;
            }
            return VibratorService.DEBUG;
        }
    }

    VibratorService(Context context) {
        vibratorOff();
        this.mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, "*vibrator*");
        this.mWakeLock.setReferenceCounted(true);
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        this.mBatteryStatsService = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mVibrations = new LinkedList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, filter);
    }

    public void systemReady() {
        this.mIm = (InputManager) this.mContext.getSystemService("input");
        this.mSettingObserver = new SettingsObserver(this.mH);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
            public void onLowPowerModeChanged(boolean enabled) {
                VibratorService.this.updateInputDeviceVibrators();
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("vibrate_input_devices"), true, this.mSettingObserver, -1);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                VibratorService.this.updateInputDeviceVibrators();
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mH);
        updateInputDeviceVibrators();
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean SelfChange) {
            VibratorService.this.updateInputDeviceVibrators();
        }
    }

    public boolean hasVibrator() {
        return doVibratorExists();
    }

    private void verifyIncomingUid(int uid) {
        if (uid != Binder.getCallingUid() && Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    public void vibrate(int uid, String opPkg, long milliseconds, int usageHint, IBinder token) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            throw new SecurityException("Requires VIBRATE permission");
        }
        verifyIncomingUid(uid);
        if (milliseconds > 0) {
            if (this.mCurrentVibration == null || !this.mCurrentVibration.hasLongerTimeout(milliseconds)) {
                Vibration vib = new Vibration(this, token, milliseconds, usageHint, uid, opPkg);
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mVibrations) {
                        removeVibrationLocked(token);
                        doCancelVibrateLocked();
                        this.mCurrentVibration = vib;
                        startVibrationLocked(vib);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    private boolean isAll0(long[] pattern) {
        for (long j : pattern) {
            if (j != 0) {
                return DEBUG;
            }
        }
        return true;
    }

    public void vibratePattern(int uid, String packageName, long[] pattern, int repeat, int usageHint, IBinder token) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            throw new SecurityException("Requires VIBRATE permission");
        }
        verifyIncomingUid(uid);
        long identity = Binder.clearCallingIdentity();
        if (pattern != null) {
            try {
                if (pattern.length != 0 && !isAll0(pattern) && repeat < pattern.length && token != null) {
                    Vibration vib = new Vibration(this, token, pattern, repeat, usageHint, uid, packageName);
                    try {
                        token.linkToDeath(vib, 0);
                        synchronized (this.mVibrations) {
                            removeVibrationLocked(token);
                            doCancelVibrateLocked();
                            if (repeat >= 0) {
                                this.mVibrations.addFirst(vib);
                                startNextVibrationLocked();
                            } else {
                                this.mCurrentVibration = vib;
                                startVibrationLocked(vib);
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void cancelVibrate(IBinder token) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "cancelVibrate");
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mVibrations) {
                Vibration vib = removeVibrationLocked(token);
                if (vib == this.mCurrentVibration) {
                    doCancelVibrateLocked();
                    startNextVibrationLocked();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void doCancelVibrateLocked() {
        if (this.mThread != null) {
            synchronized (this.mThread) {
                this.mThread.mDone = true;
                this.mThread.notify();
            }
            this.mThread = null;
        }
        doVibratorOff();
        this.mH.removeCallbacks(this.mVibrationRunnable);
        reportFinishVibrationLocked();
    }

    private void startNextVibrationLocked() {
        if (this.mVibrations.size() <= 0) {
            reportFinishVibrationLocked();
            this.mCurrentVibration = null;
        } else {
            this.mCurrentVibration = this.mVibrations.getFirst();
            startVibrationLocked(this.mCurrentVibration);
        }
    }

    private void startVibrationLocked(Vibration vib) {
        try {
            if (!this.mLowPowerMode || vib.mUsageHint == 6) {
                int mode = this.mAppOpsService.checkAudioOperation(3, vib.mUsageHint, vib.mUid, vib.mOpPkg);
                if (mode == 0) {
                    mode = this.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAppOpsService), 3, vib.mUid, vib.mOpPkg);
                }
                if (mode != 0) {
                    if (mode == 2) {
                        Slog.w(TAG, "Would be an error: vibrate from uid " + vib.mUid);
                    }
                    this.mH.post(this.mVibrationRunnable);
                    return;
                }
            } else {
                return;
            }
        } catch (RemoteException e) {
        }
        if (vib.mTimeout != 0) {
            doVibratorOn(vib.mTimeout, vib.mUid, vib.mUsageHint);
            this.mH.postDelayed(this.mVibrationRunnable, vib.mTimeout);
        } else {
            this.mThread = new VibrateThread(vib);
            this.mThread.start();
        }
    }

    private void reportFinishVibrationLocked() {
        if (this.mCurrentVibration != null) {
            try {
                this.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAppOpsService), 3, this.mCurrentVibration.mUid, this.mCurrentVibration.mOpPkg);
            } catch (RemoteException e) {
            }
            this.mCurrentVibration = null;
        }
    }

    private Vibration removeVibrationLocked(IBinder token) {
        ListIterator<Vibration> iter = this.mVibrations.listIterator(0);
        while (iter.hasNext()) {
            Vibration vib = iter.next();
            if (vib.mToken == token) {
                iter.remove();
                unlinkVibration(vib);
                return vib;
            }
        }
        if (this.mCurrentVibration != null && this.mCurrentVibration.mToken == token) {
            unlinkVibration(this.mCurrentVibration);
            return this.mCurrentVibration;
        }
        return null;
    }

    private void unlinkVibration(Vibration vib) {
        if (vib.mPattern != null) {
            vib.mToken.unlinkToDeath(vib, 0);
        }
    }

    private void updateInputDeviceVibrators() {
        synchronized (this.mVibrations) {
            doCancelVibrateLocked();
            synchronized (this.mInputDeviceVibrators) {
                this.mVibrateInputDevicesSetting = DEBUG;
                try {
                    this.mVibrateInputDevicesSetting = Settings.System.getIntForUser(this.mContext.getContentResolver(), "vibrate_input_devices", -2) > 0;
                } catch (Settings.SettingNotFoundException e) {
                }
                this.mLowPowerMode = this.mPowerManagerInternal.getLowPowerModeEnabled();
                if (this.mVibrateInputDevicesSetting) {
                    if (!this.mInputDeviceListenerRegistered) {
                        this.mInputDeviceListenerRegistered = true;
                        this.mIm.registerInputDeviceListener(this, this.mH);
                    }
                } else if (this.mInputDeviceListenerRegistered) {
                    this.mInputDeviceListenerRegistered = DEBUG;
                    this.mIm.unregisterInputDeviceListener(this);
                }
                this.mInputDeviceVibrators.clear();
                if (this.mVibrateInputDevicesSetting) {
                    int[] ids = this.mIm.getInputDeviceIds();
                    for (int i : ids) {
                        InputDevice device = this.mIm.getInputDevice(i);
                        Vibrator vibrator = device.getVibrator();
                        if (vibrator.hasVibrator()) {
                            this.mInputDeviceVibrators.add(vibrator);
                        }
                    }
                }
            }
            startNextVibrationLocked();
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateInputDeviceVibrators();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateInputDeviceVibrators();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateInputDeviceVibrators();
    }

    private boolean doVibratorExists() {
        return vibratorExists();
    }

    private void doVibratorOn(long millis, int uid, int usageHint) {
        synchronized (this.mInputDeviceVibrators) {
            try {
                this.mBatteryStatsService.noteVibratorOn(uid, millis);
                this.mCurVibUid = uid;
            } catch (RemoteException e) {
            }
            int vibratorCount = this.mInputDeviceVibrators.size();
            if (vibratorCount != 0) {
                AudioAttributes attributes = new AudioAttributes.Builder().setUsage(usageHint).build();
                for (int i = 0; i < vibratorCount; i++) {
                    this.mInputDeviceVibrators.get(i).vibrate(millis, attributes);
                }
            } else {
                vibratorOn(millis);
            }
        }
    }

    private void doVibratorOff() {
        synchronized (this.mInputDeviceVibrators) {
            if (this.mCurVibUid >= 0) {
                try {
                    this.mBatteryStatsService.noteVibratorOff(this.mCurVibUid);
                } catch (RemoteException e) {
                }
                this.mCurVibUid = -1;
            }
            int vibratorCount = this.mInputDeviceVibrators.size();
            if (vibratorCount != 0) {
                for (int i = 0; i < vibratorCount; i++) {
                    this.mInputDeviceVibrators.get(i).cancel();
                }
            } else {
                vibratorOff();
            }
        }
    }

    private class VibrateThread extends Thread {
        boolean mDone;
        final Vibration mVibration;

        VibrateThread(Vibration vib) {
            this.mVibration = vib;
            VibratorService.this.mTmpWorkSource.set(vib.mUid);
            VibratorService.this.mWakeLock.setWorkSource(VibratorService.this.mTmpWorkSource);
            VibratorService.this.mWakeLock.acquire();
        }

        private void delay(long duration) {
            if (duration > 0) {
                long bedtime = duration + SystemClock.uptimeMillis();
                do {
                    try {
                        wait(duration);
                    } catch (InterruptedException e) {
                    }
                    if (!this.mDone) {
                        duration = bedtime - SystemClock.uptimeMillis();
                    } else {
                        return;
                    }
                } while (duration > 0);
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(-8);
            synchronized (this) {
                long[] pattern = this.mVibration.mPattern;
                int len = pattern.length;
                int repeat = this.mVibration.mRepeat;
                int uid = this.mVibration.mUid;
                int usageHint = this.mVibration.mUsageHint;
                long duration = 0;
                int index = 0;
                while (!this.mDone) {
                    if (index < len) {
                        duration += pattern[index];
                        index++;
                    }
                    delay(duration);
                    if (this.mDone) {
                        break;
                    }
                    if (index < len) {
                        int index2 = index + 1;
                        duration = pattern[index];
                        if (duration > 0) {
                            VibratorService.this.doVibratorOn(duration, uid, usageHint);
                            index = index2;
                        } else {
                            index = index2;
                        }
                    } else {
                        if (repeat < 0) {
                            break;
                        }
                        duration = 0;
                        index = repeat;
                    }
                }
                VibratorService.this.mWakeLock.release();
            }
            synchronized (VibratorService.this.mVibrations) {
                if (VibratorService.this.mThread == this) {
                    VibratorService.this.mThread = null;
                }
                if (!this.mDone) {
                    VibratorService.this.mVibrations.remove(this.mVibration);
                    VibratorService.this.unlinkVibration(this.mVibration);
                    VibratorService.this.startNextVibrationLocked();
                }
            }
        }
    }
}
