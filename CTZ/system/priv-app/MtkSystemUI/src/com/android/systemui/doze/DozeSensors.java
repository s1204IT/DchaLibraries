package com.android.systemui.doze;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.doze.DozeSensors;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AlarmTimeout;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.PrintWriter;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class DozeSensors {
    private static final boolean DEBUG = DozeService.DEBUG;
    private final AlarmManager mAlarmManager;
    private final Callback mCallback;
    private final AmbientDisplayConfiguration mConfig;
    private final Context mContext;
    private final DozeParameters mDozeParameters;
    private final TriggerSensor mPickupSensor;
    private final Consumer<Boolean> mProxCallback;
    private final ProxSensor mProxSensor;
    private final ContentResolver mResolver;
    private final SensorManager mSensorManager;
    private final TriggerSensor[] mSensors;
    private final WakeLock mWakeLock;
    private final Handler mHandler = new Handler();
    private final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) { // from class: com.android.systemui.doze.DozeSensors.1
        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                for (TriggerSensor triggerSensor : DozeSensors.this.mSensors) {
                    triggerSensor.updateListener();
                }
            }
        }
    };

    public interface Callback {
        void onSensorPulse(int i, boolean z, float f, float f2);
    }

    public DozeSensors(Context context, AlarmManager alarmManager, SensorManager sensorManager, DozeParameters dozeParameters, AmbientDisplayConfiguration ambientDisplayConfiguration, WakeLock wakeLock, Callback callback, Consumer<Boolean> consumer, AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
        this.mContext = context;
        this.mAlarmManager = alarmManager;
        this.mSensorManager = sensorManager;
        this.mDozeParameters = dozeParameters;
        this.mConfig = ambientDisplayConfiguration;
        this.mWakeLock = wakeLock;
        this.mProxCallback = consumer;
        this.mResolver = this.mContext.getContentResolver();
        TriggerSensor triggerSensor = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(25), "doze_pulse_on_pick_up", ambientDisplayConfiguration.pulseOnPickupAvailable(), 3, false, false);
        this.mPickupSensor = triggerSensor;
        this.mSensors = new TriggerSensor[]{new TriggerSensor(this, this.mSensorManager.getDefaultSensor(17), null, dozeParameters.getPulseOnSigMotion(), 2, false, false), triggerSensor, new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.doubleTapSensorType()), "doze_pulse_on_double_tap", true, 4, dozeParameters.doubleTapReportsTouchCoordinates(), true), new TriggerSensor(findSensorWithType(ambientDisplayConfiguration.longPressSensorType()), "doze_pulse_on_long_press", false, true, 5, true, true)};
        this.mProxSensor = new ProxSensor(alwaysOnDisplayPolicy);
        this.mCallback = callback;
    }

    private Sensor findSensorWithType(String str) {
        return findSensorWithType(this.mSensorManager, str);
    }

    static Sensor findSensorWithType(SensorManager sensorManager, String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        for (Sensor sensor : sensorManager.getSensorList(-1)) {
            if (str.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    public void setListening(boolean z) {
        for (TriggerSensor triggerSensor : this.mSensors) {
            triggerSensor.setListening(z);
            if (z) {
                triggerSensor.registerSettingsObserver(this.mSettingsObserver);
            }
        }
        if (!z) {
            this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        }
    }

    public void setTouchscreenSensorsListening(boolean z) {
        for (TriggerSensor triggerSensor : this.mSensors) {
            if (triggerSensor.mRequiresTouchscreen) {
                triggerSensor.setListening(z);
            }
        }
    }

    public void reregisterAllSensors() {
        for (TriggerSensor triggerSensor : this.mSensors) {
            triggerSensor.setListening(false);
        }
        for (TriggerSensor triggerSensor2 : this.mSensors) {
            triggerSensor2.setListening(true);
        }
    }

    public void onUserSwitched() {
        for (TriggerSensor triggerSensor : this.mSensors) {
            triggerSensor.updateListener();
        }
    }

    public void setProxListening(boolean z) {
        this.mProxSensor.setRequested(z);
    }

    public void setDisableSensorsInterferingWithProximity(boolean z) {
        this.mPickupSensor.setDisabled(z);
    }

    public void dump(PrintWriter printWriter) {
        for (TriggerSensor triggerSensor : this.mSensors) {
            printWriter.print("Sensor: ");
            printWriter.println(triggerSensor.toString());
        }
        printWriter.print("ProxSensor: ");
        printWriter.println(this.mProxSensor.toString());
    }

    public Boolean isProximityCurrentlyFar() {
        return this.mProxSensor.mCurrentlyFar;
    }

    private class ProxSensor implements SensorEventListener {
        final AlarmTimeout mCooldownTimer;
        Boolean mCurrentlyFar;
        long mLastNear;
        final AlwaysOnDisplayPolicy mPolicy;
        boolean mRegistered;
        boolean mRequested;

        public ProxSensor(AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
            this.mPolicy = alwaysOnDisplayPolicy;
            this.mCooldownTimer = new AlarmTimeout(DozeSensors.this.mAlarmManager, new AlarmManager.OnAlarmListener() { // from class: com.android.systemui.doze.-$$Lambda$DozeSensors$ProxSensor$1rrJyrK-R8bANwbetqs61eKIcvs
                @Override // android.app.AlarmManager.OnAlarmListener
                public final void onAlarm() {
                    this.f$0.updateRegistered();
                }
            }, "prox_cooldown", DozeSensors.this.mHandler);
        }

        void setRequested(boolean z) {
            if (this.mRequested == z) {
                DozeSensors.this.mHandler.post(new Runnable() { // from class: com.android.systemui.doze.-$$Lambda$DozeSensors$ProxSensor$ocSoA7n0sI8mkM1nacSopw2_2Oc
                    @Override // java.lang.Runnable
                    public final void run() {
                        DozeSensors.ProxSensor.lambda$setRequested$0(this.f$0);
                    }
                });
            } else {
                this.mRequested = z;
                updateRegistered();
            }
        }

        public static /* synthetic */ void lambda$setRequested$0(ProxSensor proxSensor) {
            if (proxSensor.mCurrentlyFar != null) {
                DozeSensors.this.mProxCallback.accept(proxSensor.mCurrentlyFar);
            }
        }

        private void updateRegistered() {
            setRegistered(this.mRequested && !this.mCooldownTimer.isScheduled());
        }

        private void setRegistered(boolean z) {
            if (this.mRegistered == z) {
                return;
            }
            if (!z) {
                DozeSensors.this.mSensorManager.unregisterListener(this);
                this.mRegistered = false;
                this.mCurrentlyFar = null;
                return;
            }
            this.mRegistered = DozeSensors.this.mSensorManager.registerListener(this, DozeSensors.this.mSensorManager.getDefaultSensor(8), 3, DozeSensors.this.mHandler);
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (DozeSensors.DEBUG) {
                Log.d("DozeSensors", "onSensorChanged " + sensorEvent);
            }
            this.mCurrentlyFar = Boolean.valueOf(sensorEvent.values[0] >= sensorEvent.sensor.getMaximumRange());
            DozeSensors.this.mProxCallback.accept(this.mCurrentlyFar);
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (this.mCurrentlyFar != null) {
                if (!this.mCurrentlyFar.booleanValue()) {
                    this.mLastNear = jElapsedRealtime;
                } else if (this.mCurrentlyFar.booleanValue() && jElapsedRealtime - this.mLastNear < this.mPolicy.proxCooldownTriggerMs) {
                    this.mCooldownTimer.schedule(this.mPolicy.proxCooldownPeriodMs, 1);
                    updateRegistered();
                }
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public String toString() {
            return String.format("{registered=%s, requested=%s, coolingDown=%s, currentlyFar=%s}", Boolean.valueOf(this.mRegistered), Boolean.valueOf(this.mRequested), Boolean.valueOf(this.mCooldownTimer.isScheduled()), this.mCurrentlyFar);
        }
    }

    private class TriggerSensor extends TriggerEventListener {
        final boolean mConfigured;
        private boolean mDisabled;
        final int mPulseReason;
        private boolean mRegistered;
        final boolean mReportsTouchCoordinates;
        private boolean mRequested;
        final boolean mRequiresTouchscreen;
        final Sensor mSensor;
        final String mSetting;
        final boolean mSettingDefault;

        public TriggerSensor(DozeSensors dozeSensors, Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3) {
            this(sensor, str, true, z, i, z2, z3);
        }

        public TriggerSensor(Sensor sensor, String str, boolean z, boolean z2, int i, boolean z3, boolean z4) {
            this.mSensor = sensor;
            this.mSetting = str;
            this.mSettingDefault = z;
            this.mConfigured = z2;
            this.mPulseReason = i;
            this.mReportsTouchCoordinates = z3;
            this.mRequiresTouchscreen = z4;
        }

        public void setListening(boolean z) {
            if (this.mRequested == z) {
                return;
            }
            this.mRequested = z;
            updateListener();
        }

        public void setDisabled(boolean z) {
            if (this.mDisabled == z) {
                return;
            }
            this.mDisabled = z;
            updateListener();
        }

        public void updateListener() {
            if (!this.mConfigured || this.mSensor == null) {
                return;
            }
            if (this.mRequested && !this.mDisabled && enabledBySetting() && !this.mRegistered) {
                this.mRegistered = DozeSensors.this.mSensorManager.requestTriggerSensor(this, this.mSensor);
                if (DozeSensors.DEBUG) {
                    Log.d("DozeSensors", "requestTriggerSensor " + this.mRegistered);
                    return;
                }
                return;
            }
            if (this.mRegistered) {
                boolean zCancelTriggerSensor = DozeSensors.this.mSensorManager.cancelTriggerSensor(this, this.mSensor);
                if (DozeSensors.DEBUG) {
                    Log.d("DozeSensors", "cancelTriggerSensor " + zCancelTriggerSensor);
                }
                this.mRegistered = false;
            }
        }

        private boolean enabledBySetting() {
            return TextUtils.isEmpty(this.mSetting) || Settings.Secure.getIntForUser(DozeSensors.this.mResolver, this.mSetting, this.mSettingDefault ? 1 : 0, -2) != 0;
        }

        public String toString() {
            return "{mRegistered=" + this.mRegistered + ", mRequested=" + this.mRequested + ", mDisabled=" + this.mDisabled + ", mConfigured=" + this.mConfigured + ", mSensor=" + this.mSensor + "}";
        }

        @Override // android.hardware.TriggerEventListener
        public void onTrigger(final TriggerEvent triggerEvent) {
            DozeLog.traceSensor(DozeSensors.this.mContext, this.mPulseReason);
            DozeSensors.this.mHandler.post(DozeSensors.this.mWakeLock.wrap(new Runnable() { // from class: com.android.systemui.doze.-$$Lambda$DozeSensors$TriggerSensor$O2XJN2HKJ96bSF_1qNx6jPK-eFk
                @Override // java.lang.Runnable
                public final void run() {
                    DozeSensors.TriggerSensor.lambda$onTrigger$0(this.f$0, triggerEvent);
                }
            }));
        }

        public static /* synthetic */ void lambda$onTrigger$0(TriggerSensor triggerSensor, TriggerEvent triggerEvent) {
            boolean pickupSubtypePerformsProxCheck;
            float f;
            if (DozeSensors.DEBUG) {
                Log.d("DozeSensors", "onTrigger: " + triggerSensor.triggerEventToString(triggerEvent));
            }
            if (triggerSensor.mSensor.getType() == 25) {
                int i = (int) triggerEvent.values[0];
                MetricsLogger.action(DozeSensors.this.mContext, 411, i);
                pickupSubtypePerformsProxCheck = DozeSensors.this.mDozeParameters.getPickupSubtypePerformsProxCheck(i);
            } else {
                pickupSubtypePerformsProxCheck = false;
            }
            triggerSensor.mRegistered = false;
            float f2 = -1.0f;
            if (triggerSensor.mReportsTouchCoordinates && triggerEvent.values.length >= 2) {
                f2 = triggerEvent.values[0];
                f = triggerEvent.values[1];
            } else {
                f = -1.0f;
            }
            DozeSensors.this.mCallback.onSensorPulse(triggerSensor.mPulseReason, pickupSubtypePerformsProxCheck, f2, f);
            triggerSensor.updateListener();
        }

        public void registerSettingsObserver(ContentObserver contentObserver) {
            if (this.mConfigured && !TextUtils.isEmpty(this.mSetting)) {
                DozeSensors.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(this.mSetting), false, DozeSensors.this.mSettingsObserver, -1);
            }
        }

        private String triggerEventToString(TriggerEvent triggerEvent) {
            if (triggerEvent == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder("TriggerEvent[");
            sb.append(triggerEvent.timestamp);
            sb.append(',');
            sb.append(triggerEvent.sensor.getName());
            if (triggerEvent.values != null) {
                for (int i = 0; i < triggerEvent.values.length; i++) {
                    sb.append(',');
                    sb.append(triggerEvent.values[i]);
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }
}
