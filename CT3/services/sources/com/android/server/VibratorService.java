package com.android.server;

import android.R;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
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
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class VibratorService extends IVibratorService.Stub implements InputManager.InputDeviceListener {
    private static final boolean DEBUG = false;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "VibratorService";
    private final IAppOpsService mAppOpsService;
    private final IBatteryStats mBatteryStatsService;
    private final Context mContext;
    private Vibration mCurrentVibration;
    private InputManager mIm;
    private boolean mInputDeviceListenerRegistered;
    private boolean mLowPowerMode;
    private PowerManagerInternal mPowerManagerInternal;
    private final LinkedList<VibrationInfo> mPreviousVibrations;
    private final int mPreviousVibrationsLimit;
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
            if (!intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                return;
            }
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
    };

    static native boolean vibratorExists();

    static native void vibratorInit();

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

        Vibration(VibratorService this$0, IBinder token, long millis, int usageHint, int uid, String opPkg) {
            this(token, millis, null, 0, usageHint, uid, opPkg);
        }

        Vibration(VibratorService this$0, IBinder token, long[] pattern, int repeat, int usageHint, int uid, String opPkg) {
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
            return this.mTimeout != 0 && this.mStartTime + this.mTimeout >= SystemClock.uptimeMillis() + millis;
        }

        public boolean isSystemHapticFeedback() {
            return (this.mUid == 1000 || this.mUid == 0 || VibratorService.SYSTEM_UI_PACKAGE.equals(this.mOpPkg)) && this.mRepeat < 0;
        }
    }

    private static class VibrationInfo {
        String opPkg;
        long[] pattern;
        int repeat;
        long startTime;
        long timeout;
        int uid;
        int usageHint;

        public VibrationInfo(long timeout, long startTime, long[] pattern, int repeat, int usageHint, int uid, String opPkg) {
            this.timeout = timeout;
            this.startTime = startTime;
            this.pattern = pattern;
            this.repeat = repeat;
            this.usageHint = usageHint;
            this.uid = uid;
            this.opPkg = opPkg;
        }

        public String toString() {
            return "timeout: " + this.timeout + ", startTime: " + this.startTime + ", pattern: " + Arrays.toString(this.pattern) + ", repeat: " + this.repeat + ", usageHint: " + this.usageHint + ", uid: " + this.uid + ", opPkg: " + this.opPkg;
        }
    }

    VibratorService(Context context) {
        vibratorInit();
        vibratorOff();
        this.mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, "*vibrator*");
        this.mWakeLock.setReferenceCounted(true);
        this.mAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        this.mBatteryStatsService = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mPreviousVibrationsLimit = this.mContext.getResources().getInteger(R.integer.config_externalDisplayPeakRefreshRate);
        this.mVibrations = new LinkedList<>();
        this.mPreviousVibrations = new LinkedList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, filter);
    }

    public void systemReady() {
        this.mIm = (InputManager) this.mContext.getSystemService(InputManager.class);
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
        if (uid == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    public void vibrate(int uid, String opPkg, long milliseconds, int usageHint, IBinder token) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
            throw new SecurityException("Requires VIBRATE permission");
        }
        verifyIncomingUid(uid);
        if (milliseconds > 0) {
            if (this.mCurrentVibration != null && this.mCurrentVibration.hasLongerTimeout(milliseconds)) {
                return;
            }
            Vibration vib = new Vibration(this, token, milliseconds, usageHint, uid, opPkg);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mVibrations) {
                    removeVibrationLocked(token);
                    doCancelVibrateLocked();
                    addToPreviousVibrationsLocked(vib);
                    startVibrationLocked(vib);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private boolean isAll0(long[] pattern) {
        for (long j : pattern) {
            if (j != 0) {
                return false;
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
                                startVibrationLocked(vib);
                            }
                            addToPreviousVibrationsLocked(vib);
                        }
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void addToPreviousVibrationsLocked(Vibration vib) {
        if (this.mPreviousVibrations.size() > this.mPreviousVibrationsLimit) {
            this.mPreviousVibrations.removeFirst();
        }
        this.mPreviousVibrations.addLast(new VibrationInfo(vib.mTimeout, vib.mStartTime, vib.mPattern, vib.mRepeat, vib.mUsageHint, vib.mUid, vib.mOpPkg));
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
            startVibrationLocked(this.mVibrations.getFirst());
        }
    }

    private void startVibrationLocked(Vibration vib) {
        if (this.mLowPowerMode && vib.mUsageHint != 6) {
            return;
        }
        if (vib.mUsageHint == 6 && !shouldVibrateForRingtone()) {
            return;
        }
        int mode = this.mAppOpsService.checkAudioOperation(3, vib.mUsageHint, vib.mUid, vib.mOpPkg);
        if (mode == 0) {
            mode = this.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAppOpsService), 3, vib.mUid, vib.mOpPkg);
        }
        if (mode == 0) {
            this.mCurrentVibration = vib;
            if (vib.mTimeout != 0) {
                doVibratorOn(vib.mTimeout, vib.mUid, vib.mUsageHint);
                this.mH.postDelayed(this.mVibrationRunnable, vib.mTimeout);
                return;
            } else {
                this.mThread = new VibrateThread(vib);
                this.mThread.start();
                return;
            }
        }
        if (mode == 2) {
            Slog.w(TAG, "Would be an error: vibrate from uid " + vib.mUid);
        }
    }

    private boolean shouldVibrateForRingtone() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        int ringerMode = audioManager.getRingerModeInternal();
        return Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_when_ringing", 0) != 0 ? ringerMode != 0 : ringerMode == 1;
    }

    private void reportFinishVibrationLocked() {
        if (this.mCurrentVibration == null) {
            return;
        }
        try {
            this.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAppOpsService), 3, this.mCurrentVibration.mUid, this.mCurrentVibration.mOpPkg);
        } catch (RemoteException e) {
        }
        this.mCurrentVibration = null;
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
        if (this.mCurrentVibration == null || this.mCurrentVibration.mToken != token) {
            return null;
        }
        unlinkVibration(this.mCurrentVibration);
        return this.mCurrentVibration;
    }

    private void unlinkVibration(Vibration vib) {
        if (vib.mPattern == null) {
            return;
        }
        vib.mToken.unlinkToDeath(vib, 0);
    }

    private void updateInputDeviceVibrators() {
        synchronized (this.mVibrations) {
            doCancelVibrateLocked();
            synchronized (this.mInputDeviceVibrators) {
                this.mVibrateInputDevicesSetting = false;
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
                    this.mInputDeviceListenerRegistered = false;
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
            if (duration <= 0) {
                return;
            }
            long bedtime = duration + SystemClock.uptimeMillis();
            do {
                try {
                    wait(duration);
                } catch (InterruptedException e) {
                }
                if (this.mDone) {
                    return;
                } else {
                    duration = bedtime - SystemClock.uptimeMillis();
                }
            } while (duration > 0);
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
                int index = 0;
                long duration = 0;
                while (true) {
                    int index2 = index;
                    if (!this.mDone) {
                        if (index2 < len) {
                            duration += pattern[index2];
                            index2++;
                        }
                        delay(duration);
                        if (this.mDone) {
                            break;
                        }
                        if (index2 < len) {
                            index = index2 + 1;
                            duration = pattern[index2];
                            if (duration > 0) {
                                VibratorService.this.doVibratorOn(duration, uid, usageHint);
                            }
                        } else {
                            if (repeat < 0) {
                                break;
                            }
                            index = repeat;
                            duration = 0;
                        }
                    } else {
                        break;
                    }
                }
                VibratorService.this.mWakeLock.release();
            }
            synchronized (VibratorService.this.mVibrations) {
                if (VibratorService.this.mThread == this) {
                    VibratorService.this.mThread = null;
                }
                if (!this.mDone) {
                    VibratorService.this.unlinkVibration(this.mVibration);
                    VibratorService.this.startNextVibrationLocked();
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump vibrator service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Previous vibrations:");
        synchronized (this.mVibrations) {
            for (VibrationInfo info : this.mPreviousVibrations) {
                pw.print("  ");
                pw.println(info.toString());
            }
        }
    }
}
