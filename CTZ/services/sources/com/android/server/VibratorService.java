package com.android.server;

import android.R;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.icu.text.DateFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IVibratorService;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.DebugUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

public class VibratorService extends IVibratorService.Stub implements InputManager.InputDeviceListener {
    private static final boolean DEBUG = false;
    private static final long[] DOUBLE_CLICK_EFFECT_FALLBACK_TIMINGS = {0, 30, 100, 30};
    private static final long MAX_HAPTIC_FEEDBACK_DURATION = 5000;
    private static final int SCALE_HIGH = 1;
    private static final float SCALE_HIGH_GAMMA = 0.5f;
    private static final int SCALE_LOW = -1;
    private static final float SCALE_LOW_GAMMA = 1.5f;
    private static final int SCALE_LOW_MAX_AMPLITUDE = 192;
    private static final int SCALE_NONE = 0;
    private static final float SCALE_NONE_GAMMA = 1.0f;
    private static final int SCALE_VERY_HIGH = 2;
    private static final float SCALE_VERY_HIGH_GAMMA = 0.25f;
    private static final int SCALE_VERY_LOW = -2;
    private static final float SCALE_VERY_LOW_GAMMA = 2.0f;
    private static final int SCALE_VERY_LOW_MAX_AMPLITUDE = 168;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "VibratorService";
    private final boolean mAllowPriorityVibrationsInLowPowerMode;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStatsService;
    private final Context mContext;

    @GuardedBy("mLock")
    private Vibration mCurrentVibration;
    private final int mDefaultVibrationAmplitude;
    private final SparseArray<VibrationEffect> mFallbackEffects;
    private int mHapticFeedbackIntensity;
    private InputManager mIm;
    private boolean mInputDeviceListenerRegistered;
    private boolean mLowPowerMode;
    private int mNotificationIntensity;
    private PowerManagerInternal mPowerManagerInternal;
    private final LinkedList<VibrationInfo> mPreviousVibrations;
    private final int mPreviousVibrationsLimit;
    private final SparseArray<ScaleLevel> mScaleLevels;
    private SettingsObserver mSettingObserver;
    private final boolean mSupportsAmplitudeControl;
    private volatile VibrateThread mThread;
    private boolean mVibrateInputDevicesSetting;
    private Vibrator mVibrator;
    private final PowerManager.WakeLock mWakeLock;
    private final WorkSource mTmpWorkSource = new WorkSource();
    private final Handler mH = new Handler();
    private final Object mLock = new Object();
    private final ArrayList<Vibrator> mInputDeviceVibrators = new ArrayList<>();
    private int mCurVibUid = -1;
    private final Runnable mVibrationEndRunnable = new Runnable() {
        @Override
        public void run() {
            VibratorService.this.onVibrationFinished();
        }
    };
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                synchronized (VibratorService.this.mLock) {
                    if (VibratorService.this.mCurrentVibration != null && (!VibratorService.this.mCurrentVibration.isHapticFeedback() || !VibratorService.this.mCurrentVibration.isFromSystem())) {
                        VibratorService.this.doCancelVibrateLocked();
                    }
                }
            }
        }
    };

    static native boolean vibratorExists();

    static native void vibratorInit();

    static native void vibratorOff();

    static native void vibratorOn(long j);

    static native long vibratorPerformEffect(long j, long j2);

    static native void vibratorSetAmplitude(int i);

    static native boolean vibratorSupportsAmplitudeControl();

    private class Vibration implements IBinder.DeathRecipient {
        public VibrationEffect effect;
        public final String opPkg;
        public VibrationEffect originalEffect;
        public final long startTime;
        public final long startTimeDebug;
        public final IBinder token;
        public final int uid;
        public final int usageHint;

        private Vibration(IBinder iBinder, VibrationEffect vibrationEffect, int i, int i2, String str) {
            this.token = iBinder;
            this.effect = vibrationEffect;
            this.startTime = SystemClock.elapsedRealtime();
            this.startTimeDebug = System.currentTimeMillis();
            this.usageHint = i;
            this.uid = i2;
            this.opPkg = str;
        }

        @Override
        public void binderDied() {
            synchronized (VibratorService.this.mLock) {
                if (this == VibratorService.this.mCurrentVibration) {
                    VibratorService.this.doCancelVibrateLocked();
                }
            }
        }

        public boolean hasTimeoutLongerThan(long j) {
            long duration = this.effect.getDuration();
            return duration >= 0 && duration > j;
        }

        public boolean isHapticFeedback() {
            if (this.effect instanceof VibrationEffect.Prebaked) {
                switch (this.effect.getId()) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return true;
                    default:
                        Slog.w(VibratorService.TAG, "Unknown prebaked vibration effect, assuming it isn't haptic feedback.");
                        return false;
                }
            }
            long duration = this.effect.getDuration();
            return duration >= 0 && duration < VibratorService.MAX_HAPTIC_FEEDBACK_DURATION;
        }

        public boolean isNotification() {
            int i = this.usageHint;
            if (i != 5) {
                switch (i) {
                    case 7:
                    case 8:
                    case 9:
                        return true;
                    default:
                        return false;
                }
            }
            return true;
        }

        public boolean isRingtone() {
            return this.usageHint == 6;
        }

        public boolean isFromSystem() {
            return this.uid == 1000 || this.uid == 0 || VibratorService.SYSTEM_UI_PACKAGE.equals(this.opPkg);
        }

        public VibrationInfo toInfo() {
            return new VibrationInfo(this.startTimeDebug, this.effect, this.originalEffect, this.usageHint, this.uid, this.opPkg);
        }
    }

    private static class VibrationInfo {
        private final VibrationEffect mEffect;
        private final String mOpPkg;
        private final VibrationEffect mOriginalEffect;
        private final long mStartTimeDebug;
        private final int mUid;
        private final int mUsageHint;

        public VibrationInfo(long j, VibrationEffect vibrationEffect, VibrationEffect vibrationEffect2, int i, int i2, String str) {
            this.mStartTimeDebug = j;
            this.mEffect = vibrationEffect;
            this.mOriginalEffect = vibrationEffect2;
            this.mUsageHint = i;
            this.mUid = i2;
            this.mOpPkg = str;
        }

        public String toString() {
            return "startTime: " + DateFormat.getDateTimeInstance().format(new Date(this.mStartTimeDebug)) + ", effect: " + this.mEffect + ", originalEffect: " + this.mOriginalEffect + ", usageHint: " + this.mUsageHint + ", uid: " + this.mUid + ", opPkg: " + this.mOpPkg;
        }
    }

    private static final class ScaleLevel {
        public final float gamma;
        public final int maxAmplitude;

        public ScaleLevel(float f) {
            this(f, 255);
        }

        public ScaleLevel(float f, int i) {
            this.gamma = f;
            this.maxAmplitude = i;
        }

        public String toString() {
            return "ScaleLevel{gamma=" + this.gamma + ", maxAmplitude=" + this.maxAmplitude + "}";
        }
    }

    VibratorService(Context context) {
        vibratorInit();
        vibratorOff();
        this.mSupportsAmplitudeControl = vibratorSupportsAmplitudeControl();
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "*vibrator*");
        this.mWakeLock.setReferenceCounted(true);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mBatteryStatsService = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mPreviousVibrationsLimit = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceTransitionTimeIncrease);
        this.mDefaultVibrationAmplitude = this.mContext.getResources().getInteger(R.integer.config_bluetooth_operating_voltage_mv);
        this.mAllowPriorityVibrationsInLowPowerMode = this.mContext.getResources().getBoolean(R.^attr-private.activityChooserViewStyle);
        this.mPreviousVibrations = new LinkedList<>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, intentFilter);
        VibrationEffect vibrationEffectCreateEffectFromResource = createEffectFromResource(R.array.config_displayShapeArray);
        VibrationEffect vibrationEffectCreateWaveform = VibrationEffect.createWaveform(DOUBLE_CLICK_EFFECT_FALLBACK_TIMINGS, -1);
        VibrationEffect vibrationEffectCreateEffectFromResource2 = createEffectFromResource(R.array.config_cameraPrivacyLightColors);
        VibrationEffect vibrationEffectCreateEffectFromResource3 = createEffectFromResource(R.array.config_autoRotationTiltTolerance);
        this.mFallbackEffects = new SparseArray<>();
        this.mFallbackEffects.put(0, vibrationEffectCreateEffectFromResource);
        this.mFallbackEffects.put(1, vibrationEffectCreateWaveform);
        this.mFallbackEffects.put(2, vibrationEffectCreateEffectFromResource3);
        this.mFallbackEffects.put(5, vibrationEffectCreateEffectFromResource2);
        this.mScaleLevels = new SparseArray<>();
        this.mScaleLevels.put(-2, new ScaleLevel(SCALE_VERY_LOW_GAMMA, SCALE_VERY_LOW_MAX_AMPLITUDE));
        this.mScaleLevels.put(-1, new ScaleLevel(SCALE_LOW_GAMMA, SCALE_LOW_MAX_AMPLITUDE));
        this.mScaleLevels.put(0, new ScaleLevel(1.0f));
        this.mScaleLevels.put(1, new ScaleLevel(0.5f));
        this.mScaleLevels.put(2, new ScaleLevel(SCALE_VERY_HIGH_GAMMA));
    }

    private VibrationEffect createEffectFromResource(int i) {
        return createEffectFromTimings(getLongIntArray(this.mContext.getResources(), i));
    }

    private static VibrationEffect createEffectFromTimings(long[] jArr) {
        if (jArr == null || jArr.length == 0) {
            return null;
        }
        if (jArr.length == 1) {
            return VibrationEffect.createOneShot(jArr[0], -1);
        }
        return VibrationEffect.createWaveform(jArr, -1);
    }

    public void systemReady() {
        Trace.traceBegin(8388608L, "VibratorService#systemReady");
        try {
            this.mIm = (InputManager) this.mContext.getSystemService(InputManager.class);
            this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
            this.mSettingObserver = new SettingsObserver(this.mH);
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            this.mPowerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() {
                public int getServiceType() {
                    return 2;
                }

                public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
                    VibratorService.this.updateVibrators();
                }
            });
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("vibrate_input_devices"), true, this.mSettingObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("haptic_feedback_intensity"), true, this.mSettingObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("notification_vibration_intensity"), true, this.mSettingObserver, -1);
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    VibratorService.this.updateVibrators();
                }
            }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mH);
            updateVibrators();
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            VibratorService.this.updateVibrators();
        }
    }

    public boolean hasVibrator() {
        return doVibratorExists();
    }

    public boolean hasAmplitudeControl() {
        boolean z;
        synchronized (this.mInputDeviceVibrators) {
            z = this.mSupportsAmplitudeControl && this.mInputDeviceVibrators.isEmpty();
        }
        return z;
    }

    private void verifyIncomingUid(int i) {
        if (i == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private static boolean verifyVibrationEffect(VibrationEffect vibrationEffect) {
        if (vibrationEffect == null) {
            Slog.wtf(TAG, "effect must not be null");
            return false;
        }
        try {
            vibrationEffect.validate();
            return true;
        } catch (Exception e) {
            Slog.wtf(TAG, "Encountered issue when verifying VibrationEffect.", e);
            return false;
        }
    }

    private static long[] getLongIntArray(Resources resources, int i) {
        int[] intArray = resources.getIntArray(i);
        if (intArray == null) {
            return null;
        }
        long[] jArr = new long[intArray.length];
        for (int i2 = 0; i2 < intArray.length; i2++) {
            jArr[i2] = intArray[i2];
        }
        return jArr;
    }

    public void vibrate(int i, String str, VibrationEffect vibrationEffect, int i2, IBinder iBinder) {
        Trace.traceBegin(8388608L, "vibrate");
        try {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
                throw new SecurityException("Requires VIBRATE permission");
            }
            if (iBinder == null) {
                Slog.e(TAG, "token must not be null");
                return;
            }
            verifyIncomingUid(i);
            if (verifyVibrationEffect(vibrationEffect)) {
                synchronized (this.mLock) {
                    if ((vibrationEffect instanceof VibrationEffect.OneShot) && this.mCurrentVibration != null && (this.mCurrentVibration.effect instanceof VibrationEffect.OneShot)) {
                        VibrationEffect.OneShot oneShot = (VibrationEffect.OneShot) vibrationEffect;
                        VibrationEffect.OneShot oneShot2 = this.mCurrentVibration.effect;
                        if (this.mCurrentVibration.hasTimeoutLongerThan(oneShot.getDuration()) && oneShot.getAmplitude() == oneShot2.getAmplitude()) {
                            return;
                        }
                    }
                    if (isRepeatingVibration(vibrationEffect) || this.mCurrentVibration == null || !isRepeatingVibration(this.mCurrentVibration.effect)) {
                        Vibration vibration = new Vibration(iBinder, vibrationEffect, i2, i, str);
                        linkVibration(vibration);
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            doCancelVibrateLocked();
                            startVibrationLocked(vibration);
                            addToPreviousVibrationsLocked(vibration);
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    }
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private static boolean isRepeatingVibration(VibrationEffect vibrationEffect) {
        return vibrationEffect.getDuration() == JobStatus.NO_LATEST_RUNTIME;
    }

    private void addToPreviousVibrationsLocked(Vibration vibration) {
        if (this.mPreviousVibrations.size() > this.mPreviousVibrationsLimit) {
            this.mPreviousVibrations.removeFirst();
        }
        this.mPreviousVibrations.addLast(vibration.toInfo());
    }

    public void cancelVibrate(IBinder iBinder) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "cancelVibrate");
        synchronized (this.mLock) {
            if (this.mCurrentVibration != null && this.mCurrentVibration.token == iBinder) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    doCancelVibrateLocked();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void doCancelVibrateLocked() {
        Trace.asyncTraceEnd(8388608L, "vibration", 0);
        Trace.traceBegin(8388608L, "doCancelVibrateLocked");
        try {
            this.mH.removeCallbacks(this.mVibrationEndRunnable);
            if (this.mThread != null) {
                this.mThread.cancel();
                this.mThread = null;
            }
            doVibratorOff();
            reportFinishVibrationLocked();
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    public void onVibrationFinished() {
        synchronized (this.mLock) {
            doCancelVibrateLocked();
        }
    }

    @GuardedBy("mLock")
    private void startVibrationLocked(Vibration vibration) {
        Trace.traceBegin(8388608L, "startVibrationLocked");
        try {
            if (isAllowedToVibrateLocked(vibration)) {
                int currentIntensityLocked = getCurrentIntensityLocked(vibration);
                if (currentIntensityLocked == 0) {
                    return;
                }
                if (!vibration.isRingtone() || shouldVibrateForRingtone()) {
                    int appOpMode = getAppOpMode(vibration);
                    if (appOpMode == 0) {
                        applyVibrationIntensityScalingLocked(vibration, currentIntensityLocked);
                        startVibrationInnerLocked(vibration);
                        return;
                    }
                    if (appOpMode == 2) {
                        Slog.w(TAG, "Would be an error: vibrate from uid " + vibration.uid);
                    }
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    @GuardedBy("mLock")
    private void startVibrationInnerLocked(Vibration vibration) {
        Trace.traceBegin(8388608L, "startVibrationInnerLocked");
        try {
            this.mCurrentVibration = vibration;
            if (vibration.effect instanceof VibrationEffect.OneShot) {
                Trace.asyncTraceBegin(8388608L, "vibration", 0);
                VibrationEffect.OneShot oneShot = vibration.effect;
                doVibratorOn(oneShot.getDuration(), oneShot.getAmplitude(), vibration.uid, vibration.usageHint);
                this.mH.postDelayed(this.mVibrationEndRunnable, oneShot.getDuration());
            } else if (vibration.effect instanceof VibrationEffect.Waveform) {
                Trace.asyncTraceBegin(8388608L, "vibration", 0);
                this.mThread = new VibrateThread(vibration.effect, vibration.uid, vibration.usageHint);
                this.mThread.start();
            } else if (vibration.effect instanceof VibrationEffect.Prebaked) {
                Trace.asyncTraceBegin(8388608L, "vibration", 0);
                long jDoVibratorPrebakedEffectLocked = doVibratorPrebakedEffectLocked(vibration);
                if (jDoVibratorPrebakedEffectLocked > 0) {
                    this.mH.postDelayed(this.mVibrationEndRunnable, jDoVibratorPrebakedEffectLocked);
                }
            } else {
                Slog.e(TAG, "Unknown vibration type, ignoring");
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private boolean isAllowedToVibrateLocked(Vibration vibration) {
        return !this.mLowPowerMode || vibration.usageHint == 6 || vibration.usageHint == 4 || vibration.usageHint == 11 || vibration.usageHint == 7;
    }

    private int getCurrentIntensityLocked(Vibration vibration) {
        if (vibration.isNotification() || vibration.isRingtone()) {
            return this.mNotificationIntensity;
        }
        if (vibration.isHapticFeedback()) {
            return this.mHapticFeedbackIntensity;
        }
        return 2;
    }

    private void applyVibrationIntensityScalingLocked(Vibration vibration, int i) {
        int defaultNotificationVibrationIntensity;
        if (vibration.effect instanceof VibrationEffect.Prebaked) {
            vibration.effect.setEffectStrength(intensityToEffectStrength(i));
            return;
        }
        if (vibration.isNotification() || vibration.isRingtone()) {
            defaultNotificationVibrationIntensity = this.mVibrator.getDefaultNotificationVibrationIntensity();
        } else if (vibration.isHapticFeedback()) {
            defaultNotificationVibrationIntensity = this.mVibrator.getDefaultHapticFeedbackIntensity();
        } else {
            return;
        }
        ScaleLevel scaleLevel = this.mScaleLevels.get(i - defaultNotificationVibrationIntensity);
        if (scaleLevel == null) {
            Slog.e(TAG, "No configured scaling level! (current=" + i + ", default= " + defaultNotificationVibrationIntensity + ")");
            return;
        }
        VibrationEffect vibrationEffectScale = null;
        if (vibration.effect instanceof VibrationEffect.OneShot) {
            vibrationEffectScale = vibration.effect.resolve(this.mDefaultVibrationAmplitude).scale(scaleLevel.gamma, scaleLevel.maxAmplitude);
        } else if (vibration.effect instanceof VibrationEffect.Waveform) {
            vibrationEffectScale = vibration.effect.resolve(this.mDefaultVibrationAmplitude).scale(scaleLevel.gamma, scaleLevel.maxAmplitude);
        } else {
            Slog.w(TAG, "Unable to apply intensity scaling, unknown VibrationEffect type");
        }
        if (vibrationEffectScale != null) {
            vibration.originalEffect = vibration.effect;
            vibration.effect = vibrationEffectScale;
        }
    }

    private boolean shouldVibrateForRingtone() {
        int ringerModeInternal = ((AudioManager) this.mContext.getSystemService(AudioManager.class)).getRingerModeInternal();
        return Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_when_ringing", 0) != 0 ? ringerModeInternal != 0 : ringerModeInternal == 1;
    }

    private int getAppOpMode(Vibration vibration) {
        int iCheckAudioOpNoThrow = this.mAppOps.checkAudioOpNoThrow(3, vibration.usageHint, vibration.uid, vibration.opPkg);
        if (iCheckAudioOpNoThrow == 0) {
            return this.mAppOps.startOpNoThrow(3, vibration.uid, vibration.opPkg);
        }
        return iCheckAudioOpNoThrow;
    }

    @GuardedBy("mLock")
    private void reportFinishVibrationLocked() {
        Trace.traceBegin(8388608L, "reportFinishVibrationLocked");
        try {
            if (this.mCurrentVibration != null) {
                this.mAppOps.finishOp(3, this.mCurrentVibration.uid, this.mCurrentVibration.opPkg);
                unlinkVibration(this.mCurrentVibration);
                this.mCurrentVibration = null;
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private void linkVibration(Vibration vibration) {
        if (vibration.effect instanceof VibrationEffect.Waveform) {
            try {
                vibration.token.linkToDeath(vibration, 0);
            } catch (RemoteException e) {
            }
        }
    }

    private void unlinkVibration(Vibration vibration) {
        if (vibration.effect instanceof VibrationEffect.Waveform) {
            vibration.token.unlinkToDeath(vibration, 0);
        }
    }

    private void updateVibrators() {
        synchronized (this.mLock) {
            boolean zUpdateInputDeviceVibratorsLocked = updateInputDeviceVibratorsLocked();
            boolean zUpdateLowPowerModeLocked = updateLowPowerModeLocked();
            updateVibrationIntensityLocked();
            if (zUpdateInputDeviceVibratorsLocked || zUpdateLowPowerModeLocked) {
                doCancelVibrateLocked();
            }
        }
    }

    private boolean updateInputDeviceVibratorsLocked() {
        boolean z;
        boolean z2;
        try {
            z = Settings.System.getIntForUser(this.mContext.getContentResolver(), "vibrate_input_devices", -2) > 0;
        } catch (Settings.SettingNotFoundException e) {
            z = false;
        }
        if (z != this.mVibrateInputDevicesSetting) {
            this.mVibrateInputDevicesSetting = z;
            z2 = true;
        } else {
            z2 = false;
        }
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
            for (int i : this.mIm.getInputDeviceIds()) {
                Vibrator vibrator = this.mIm.getInputDevice(i).getVibrator();
                if (vibrator.hasVibrator()) {
                    this.mInputDeviceVibrators.add(vibrator);
                }
            }
            return true;
        }
        return z2;
    }

    private boolean updateLowPowerModeLocked() {
        boolean z = this.mPowerManagerInternal.getLowPowerState(2).batterySaverEnabled;
        if (z != this.mLowPowerMode) {
            this.mLowPowerMode = z;
            return true;
        }
        return false;
    }

    private void updateVibrationIntensityLocked() {
        this.mHapticFeedbackIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_intensity", this.mVibrator.getDefaultHapticFeedbackIntensity(), -2);
        this.mNotificationIntensity = Settings.System.getIntForUser(this.mContext.getContentResolver(), "notification_vibration_intensity", this.mVibrator.getDefaultNotificationVibrationIntensity(), -2);
    }

    @Override
    public void onInputDeviceAdded(int i) {
        updateVibrators();
    }

    @Override
    public void onInputDeviceChanged(int i) {
        updateVibrators();
    }

    @Override
    public void onInputDeviceRemoved(int i) {
        updateVibrators();
    }

    private boolean doVibratorExists() {
        return vibratorExists();
    }

    private void doVibratorOn(long j, int i, int i2, int i3) {
        Trace.traceBegin(8388608L, "doVibratorOn");
        try {
            synchronized (this.mInputDeviceVibrators) {
                if (i == -1) {
                    try {
                        i = this.mDefaultVibrationAmplitude;
                    } finally {
                    }
                }
                noteVibratorOnLocked(i2, j);
                int size = this.mInputDeviceVibrators.size();
                if (size != 0) {
                    AudioAttributes audioAttributesBuild = new AudioAttributes.Builder().setUsage(i3).build();
                    for (int i4 = 0; i4 < size; i4++) {
                        this.mInputDeviceVibrators.get(i4).vibrate(j, audioAttributesBuild);
                    }
                } else {
                    vibratorOn(j);
                    doVibratorSetAmplitude(i);
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private void doVibratorSetAmplitude(int i) {
        if (this.mSupportsAmplitudeControl) {
            vibratorSetAmplitude(i);
        }
    }

    private void doVibratorOff() {
        Trace.traceBegin(8388608L, "doVibratorOff");
        try {
            synchronized (this.mInputDeviceVibrators) {
                noteVibratorOffLocked();
                int size = this.mInputDeviceVibrators.size();
                if (size != 0) {
                    for (int i = 0; i < size; i++) {
                        this.mInputDeviceVibrators.get(i).cancel();
                    }
                } else {
                    vibratorOff();
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    @GuardedBy("mLock")
    private long doVibratorPrebakedEffectLocked(Vibration vibration) {
        boolean z;
        Trace.traceBegin(8388608L, "doVibratorPrebakedEffectLocked");
        try {
            VibrationEffect.Prebaked prebaked = vibration.effect;
            synchronized (this.mInputDeviceVibrators) {
                z = !this.mInputDeviceVibrators.isEmpty();
            }
            if (!z) {
                long jVibratorPerformEffect = vibratorPerformEffect(prebaked.getId(), prebaked.getEffectStrength());
                if (jVibratorPerformEffect > 0) {
                    noteVibratorOnLocked(vibration.uid, jVibratorPerformEffect);
                    return jVibratorPerformEffect;
                }
            }
            if (!prebaked.shouldFallback()) {
                return 0L;
            }
            VibrationEffect fallbackEffect = getFallbackEffect(prebaked.getId());
            if (fallbackEffect == null) {
                Slog.w(TAG, "Failed to play prebaked effect, no fallback");
                return 0L;
            }
            Vibration vibration2 = new Vibration(vibration.token, fallbackEffect, vibration.usageHint, vibration.uid, vibration.opPkg);
            int currentIntensityLocked = getCurrentIntensityLocked(vibration2);
            linkVibration(vibration2);
            applyVibrationIntensityScalingLocked(vibration2, currentIntensityLocked);
            startVibrationInnerLocked(vibration2);
            return 0L;
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private VibrationEffect getFallbackEffect(int i) {
        return this.mFallbackEffects.get(i);
    }

    private static int intensityToEffectStrength(int i) {
        switch (i) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Slog.w(TAG, "Got unexpected vibration intensity: " + i);
                break;
        }
        return 2;
    }

    private void noteVibratorOnLocked(int i, long j) {
        try {
            this.mBatteryStatsService.noteVibratorOn(i, j);
            this.mCurVibUid = i;
        } catch (RemoteException e) {
        }
    }

    private void noteVibratorOffLocked() {
        if (this.mCurVibUid >= 0) {
            try {
                this.mBatteryStatsService.noteVibratorOff(this.mCurVibUid);
            } catch (RemoteException e) {
            }
            this.mCurVibUid = -1;
        }
    }

    private class VibrateThread extends Thread {
        private boolean mForceStop;
        private final int mUid;
        private final int mUsageHint;
        private final VibrationEffect.Waveform mWaveform;

        VibrateThread(VibrationEffect.Waveform waveform, int i, int i2) {
            this.mWaveform = waveform;
            this.mUid = i;
            this.mUsageHint = i2;
            VibratorService.this.mTmpWorkSource.set(i);
            VibratorService.this.mWakeLock.setWorkSource(VibratorService.this.mTmpWorkSource);
        }

        private long delayLocked(long j) {
            Trace.traceBegin(8388608L, "delayLocked");
            if (j <= 0) {
                return 0L;
            }
            try {
                long jUptimeMillis = SystemClock.uptimeMillis() + j;
                long jUptimeMillis2 = j;
                do {
                    try {
                        wait(jUptimeMillis2);
                    } catch (InterruptedException e) {
                    }
                    if (this.mForceStop) {
                        break;
                    }
                    jUptimeMillis2 = jUptimeMillis - SystemClock.uptimeMillis();
                } while (jUptimeMillis2 > 0);
                return j - jUptimeMillis2;
            } finally {
                Trace.traceEnd(8388608L);
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(-8);
            VibratorService.this.mWakeLock.acquire();
            try {
                if (playWaveform()) {
                    VibratorService.this.onVibrationFinished();
                }
            } finally {
                VibratorService.this.mWakeLock.release();
            }
        }

        public boolean playWaveform() {
            boolean z;
            long j;
            Trace.traceBegin(8388608L, "playWaveform");
            try {
                synchronized (this) {
                    long[] timings = this.mWaveform.getTimings();
                    int[] amplitudes = this.mWaveform.getAmplitudes();
                    int length = timings.length;
                    int repeatIndex = this.mWaveform.getRepeatIndex();
                    int i = 0;
                    long j2 = 0;
                    long totalOnDuration = 0;
                    while (!this.mForceStop) {
                        if (i < length) {
                            int i2 = amplitudes[i];
                            int i3 = i + 1;
                            long j3 = timings[i];
                            if (j3 <= j2) {
                                i = i3;
                            } else {
                                if (i2 == 0) {
                                    j = j3;
                                } else if (totalOnDuration <= j2) {
                                    totalOnDuration = getTotalOnDuration(timings, amplitudes, i3 - 1, repeatIndex);
                                    j = j3;
                                    VibratorService.this.doVibratorOn(totalOnDuration, i2, this.mUid, this.mUsageHint);
                                } else {
                                    j = j3;
                                    VibratorService.this.doVibratorSetAmplitude(i2);
                                }
                                long jDelayLocked = delayLocked(j);
                                if (i2 != 0) {
                                    totalOnDuration -= jDelayLocked;
                                }
                                i = i3;
                            }
                        } else {
                            if (repeatIndex < 0) {
                                break;
                            }
                            i = repeatIndex;
                        }
                        j2 = 0;
                    }
                    z = !this.mForceStop;
                }
                return z;
            } finally {
                Trace.traceEnd(8388608L);
            }
        }

        public void cancel() {
            synchronized (this) {
                VibratorService.this.mThread.mForceStop = true;
                VibratorService.this.mThread.notify();
            }
        }

        private long getTotalOnDuration(long[] jArr, int[] iArr, int i, int i2) {
            long j = 0;
            int i3 = i;
            while (iArr[i3] != 0) {
                int i4 = i3 + 1;
                j += jArr[i3];
                if (i4 < jArr.length) {
                    i3 = i4;
                } else {
                    if (i2 < 0) {
                        break;
                    }
                    i3 = i2;
                }
                if (i3 == i) {
                    return 1000L;
                }
            }
            return j;
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("Vibrator Service:");
            synchronized (this.mLock) {
                printWriter.print("  mCurrentVibration=");
                if (this.mCurrentVibration != null) {
                    printWriter.println(this.mCurrentVibration.toInfo().toString());
                } else {
                    printWriter.println("null");
                }
                printWriter.println("  mLowPowerMode=" + this.mLowPowerMode);
                printWriter.println("  mHapticFeedbackIntensity=" + this.mHapticFeedbackIntensity);
                printWriter.println("  mNotificationIntensity=" + this.mNotificationIntensity);
                printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                printWriter.println("  Previous vibrations:");
                for (VibrationInfo vibrationInfo : this.mPreviousVibrations) {
                    printWriter.print("    ");
                    printWriter.println(vibrationInfo.toString());
                }
            }
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        new VibratorShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    private final class VibratorShellCommand extends ShellCommand {
        private static final long MAX_VIBRATION_MS = 200;
        private final IBinder mToken;

        private VibratorShellCommand(IBinder iBinder) {
            this.mToken = iBinder;
        }

        public int onCommand(String str) {
            if ("vibrate".equals(str)) {
                return runVibrate();
            }
            return handleDefaultCommands(str);
        }

        private int runVibrate() {
            Trace.traceBegin(8388608L, "runVibrate");
            try {
                try {
                    int i = Settings.Global.getInt(VibratorService.this.mContext.getContentResolver(), "zen_mode");
                    if (i != 0) {
                        PrintWriter outPrintWriter = getOutPrintWriter();
                        Throwable th = null;
                        try {
                            try {
                                outPrintWriter.print("Ignoring because device is on DND mode ");
                                outPrintWriter.println(DebugUtils.flagsToString(Settings.Global.class, "ZEN_MODE_", i));
                                return 0;
                            } finally {
                            }
                        } finally {
                            if (outPrintWriter != null) {
                                $closeResource(th, outPrintWriter);
                            }
                        }
                    }
                } catch (Settings.SettingNotFoundException e) {
                }
                long j = Long.parseLong(getNextArgRequired());
                if (j > MAX_VIBRATION_MS) {
                    throw new IllegalArgumentException("maximum duration is 200");
                }
                String nextArg = getNextArg();
                if (nextArg == null) {
                    nextArg = "Shell command";
                }
                VibratorService.this.vibrate(Binder.getCallingUid(), nextArg, VibrationEffect.createOneShot(j, -1), 0, this.mToken);
                return 0;
            } finally {
                Trace.traceEnd(8388608L);
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        public void onHelp() throws Exception {
            PrintWriter outPrintWriter = getOutPrintWriter();
            try {
                outPrintWriter.println("Vibrator commands:");
                outPrintWriter.println("  help");
                outPrintWriter.println("    Prints this help text.");
                outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                outPrintWriter.println("  vibrate duration [description]");
                outPrintWriter.println("    Vibrates for duration milliseconds; ignored when device is on DND ");
                outPrintWriter.println("    (Do Not Disturb) mode.");
                outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            } finally {
                if (outPrintWriter != null) {
                    $closeResource(null, outPrintWriter);
                }
            }
        }
    }
}
