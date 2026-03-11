package com.android.systemui.doze;

import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.util.Preconditions;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.doze.DozeSensors;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.Assert;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class DozeTriggers implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    private final boolean mAllowPulseTriggers;
    private final AmbientDisplayConfiguration mConfig;
    private final Context mContext;
    private final DozeHost mDozeHost;
    private final DozeParameters mDozeParameters;
    private final DozeSensors mDozeSensors;
    private final Handler mHandler;
    private final DozeMachine mMachine;
    private long mNotificationPulseTime;
    private boolean mPulsePending;
    private final SensorManager mSensorManager;
    private final UiModeManager mUiModeManager;
    private final WakeLock mWakeLock;
    private final TriggerReceiver mBroadcastReceiver = new TriggerReceiver();
    private DozeHost.Callback mHostCallback = new DozeHost.Callback() {
        @Override
        public void onNotificationHeadsUp() {
            DozeTriggers.this.onNotification();
        }

        @Override
        public void onPowerSaveChanged(boolean z) {
            if (z) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
        }
    };

    public DozeTriggers(Context context, DozeMachine dozeMachine, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, SensorManager sensorManager, Handler handler, WakeLock wakeLock, boolean z) {
        this.mContext = context;
        this.mMachine = dozeMachine;
        this.mDozeHost = dozeHost;
        this.mConfig = ambientDisplayConfiguration;
        this.mDozeParameters = dozeParameters;
        this.mSensorManager = sensorManager;
        this.mHandler = handler;
        this.mWakeLock = wakeLock;
        this.mAllowPulseTriggers = z;
        this.mDozeSensors = new DozeSensors(context, alarmManager, this.mSensorManager, dozeParameters, ambientDisplayConfiguration, wakeLock, new DozeSensors.Callback() {
            @Override
            public final void onSensorPulse(int i, boolean z2, float f, float f2) {
                this.f$0.onSensor(i, z2, f, f2);
            }
        }, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.onProximityFar(((Boolean) obj).booleanValue());
            }
        }, dozeParameters.getPolicy());
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
    }

    public void onNotification() {
        if (DozeMachine.DEBUG) {
            Log.d("DozeTriggers", "requestNotificationPulse");
        }
        this.mNotificationPulseTime = SystemClock.elapsedRealtime();
        if (this.mConfig.pulseOnNotificationEnabled(-2)) {
            requestPulse(1, false);
            DozeLog.traceNotificationPulse(this.mContext);
        }
    }

    private void proximityCheckThenCall(final IntConsumer intConsumer, boolean z, final int i) {
        Boolean boolIsProximityCurrentlyFar = this.mDozeSensors.isProximityCurrentlyFar();
        if (z) {
            intConsumer.accept(3);
        } else if (boolIsProximityCurrentlyFar != null) {
            intConsumer.accept(boolIsProximityCurrentlyFar.booleanValue() ? 2 : 1);
        } else {
            final long jUptimeMillis = SystemClock.uptimeMillis();
            new ProximityCheck() {
                {
                    super();
                }

                @Override
                public void onProximityResult(int i2) {
                    DozeLog.traceProximityResult(DozeTriggers.this.mContext, i2 == 1, SystemClock.uptimeMillis() - jUptimeMillis, i);
                    intConsumer.accept(i2);
                }
            }.check();
        }
    }

    public void onSensor(int i, boolean z, final float f, final float f2) {
        boolean z2 = false;
        final boolean z3 = i == 4;
        boolean z4 = i == 3;
        boolean z5 = i == 5;
        if (this.mConfig.alwaysOnEnabled(-2) && !z5) {
            proximityCheckThenCall(new IntConsumer() {
                @Override
                public final void accept(int i2) {
                    DozeTriggers.lambda$onSensor$0(this.f$0, z3, f, f2, i2);
                }
            }, z, i);
            return;
        }
        requestPulse(i, z);
        if (z4) {
            if (SystemClock.elapsedRealtime() - this.mNotificationPulseTime < this.mDozeParameters.getPickupVibrationThreshold()) {
                z2 = true;
            }
            DozeLog.tracePickupPulse(this.mContext, z2);
        }
    }

    public static void lambda$onSensor$0(DozeTriggers dozeTriggers, boolean z, float f, float f2, int i) {
        if (i == 1) {
            return;
        }
        if (z) {
            dozeTriggers.mDozeHost.onDoubleTap(f, f2);
            dozeTriggers.mMachine.wakeUp();
        } else {
            dozeTriggers.mDozeHost.extendPulse();
        }
    }

    public void onProximityFar(boolean z) {
        boolean z2 = !z;
        DozeMachine.State state = this.mMachine.getState();
        boolean z3 = state == DozeMachine.State.DOZE_AOD_PAUSED;
        boolean z4 = state == DozeMachine.State.DOZE_AOD_PAUSING;
        boolean z5 = state == DozeMachine.State.DOZE_AOD;
        if (state == DozeMachine.State.DOZE_PULSING) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox changed, ignore touch = " + z2);
            }
            this.mDozeHost.onIgnoreTouchWhilePulsing(z2);
        }
        if (z && (z3 || z4)) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox FAR, unpausing AOD");
            }
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
        } else if (z2 && z5) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox NEAR, pausing AOD");
            }
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD_PAUSING);
        }
    }

    @Override
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        switch (state2) {
            case INITIALIZED:
                this.mBroadcastReceiver.register(this.mContext);
                this.mDozeHost.addCallback(this.mHostCallback);
                checkTriggersAtInit();
                break;
            case DOZE:
            case DOZE_AOD:
                this.mDozeSensors.setProxListening(state2 != DozeMachine.State.DOZE);
                if (state != DozeMachine.State.INITIALIZED) {
                    this.mDozeSensors.reregisterAllSensors();
                }
                this.mDozeSensors.setListening(true);
                break;
            case DOZE_AOD_PAUSED:
            case DOZE_AOD_PAUSING:
                this.mDozeSensors.setProxListening(true);
                this.mDozeSensors.setListening(false);
                break;
            case DOZE_PULSING:
                this.mDozeSensors.setTouchscreenSensorsListening(false);
                this.mDozeSensors.setProxListening(true);
                break;
            case FINISH:
                this.mBroadcastReceiver.unregister(this.mContext);
                this.mDozeHost.removeCallback(this.mHostCallback);
                this.mDozeSensors.setListening(false);
                this.mDozeSensors.setProxListening(false);
                break;
        }
    }

    private void checkTriggersAtInit() {
        if (this.mUiModeManager.getCurrentModeType() == 3 || this.mDozeHost.isPowerSaveActive() || this.mDozeHost.isBlockingDoze() || !this.mDozeHost.isProvisioned()) {
            this.mMachine.requestState(DozeMachine.State.FINISH);
        }
    }

    public void requestPulse(final int i, boolean z) {
        Assert.isMainThread();
        this.mDozeHost.extendPulse();
        if (this.mPulsePending || !this.mAllowPulseTriggers || !canPulse()) {
            if (this.mAllowPulseTriggers) {
                DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
                return;
            }
            return;
        }
        boolean z2 = true;
        this.mPulsePending = true;
        IntConsumer intConsumer = new IntConsumer() {
            @Override
            public final void accept(int i2) {
                DozeTriggers.lambda$requestPulse$1(this.f$0, i, i2);
            }
        };
        if (this.mDozeParameters.getProxCheckBeforePulse() && !z) {
            z2 = false;
        }
        proximityCheckThenCall(intConsumer, z2, i);
    }

    public static void lambda$requestPulse$1(DozeTriggers dozeTriggers, int i, int i2) {
        if (i2 == 1) {
            dozeTriggers.mPulsePending = false;
        } else {
            dozeTriggers.continuePulseRequest(i);
        }
    }

    private boolean canPulse() {
        return this.mMachine.getState() == DozeMachine.State.DOZE || this.mMachine.getState() == DozeMachine.State.DOZE_AOD;
    }

    private void continuePulseRequest(int i) {
        this.mPulsePending = false;
        if (this.mDozeHost.isPulsingBlocked() || !canPulse()) {
            DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
        } else {
            this.mMachine.requestPulse(i);
        }
    }

    @Override
    public void dump(PrintWriter printWriter) {
        printWriter.print(" notificationPulseTime=");
        printWriter.println(Formatter.formatShortElapsedTime(this.mContext, this.mNotificationPulseTime));
        printWriter.print(" pulsePending=");
        printWriter.println(this.mPulsePending);
        printWriter.println("DozeSensors:");
        this.mDozeSensors.dump(printWriter);
    }

    private abstract class ProximityCheck implements SensorEventListener, Runnable {
        private boolean mFinished;
        private float mMaxRange;
        private boolean mRegistered;

        protected abstract void onProximityResult(int i);

        private ProximityCheck() {
        }

        public void check() {
            Preconditions.checkState((this.mFinished || this.mRegistered) ? false : true);
            Sensor defaultSensor = DozeTriggers.this.mSensorManager.getDefaultSensor(8);
            if (defaultSensor != null) {
                DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(true);
                this.mMaxRange = defaultSensor.getMaximumRange();
                DozeTriggers.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeTriggers.this.mHandler);
                DozeTriggers.this.mHandler.postDelayed(this, 500L);
                DozeTriggers.this.mWakeLock.acquire();
                this.mRegistered = true;
                return;
            }
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: No sensor found");
            }
            finishWithResult(0);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.values.length == 0) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "ProxCheck: Event has no values!");
                }
                finishWithResult(0);
                return;
            }
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: Event: value=" + sensorEvent.values[0] + " max=" + this.mMaxRange);
            }
            finishWithResult(sensorEvent.values[0] < this.mMaxRange ? 1 : 2);
        }

        @Override
        public void run() {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: No event received before timeout");
            }
            finishWithResult(0);
        }

        private void finishWithResult(int i) {
            if (this.mFinished) {
                return;
            }
            boolean z = this.mRegistered;
            if (this.mRegistered) {
                DozeTriggers.this.mHandler.removeCallbacks(this);
                DozeTriggers.this.mSensorManager.unregisterListener(this);
                DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(false);
                this.mRegistered = false;
            }
            onProximityResult(i);
            if (z) {
                DozeTriggers.this.mWakeLock.release();
            }
            this.mFinished = true;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class TriggerReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private TriggerReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.doze.pulse".equals(intent.getAction())) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "Received pulse intent");
                }
                DozeTriggers.this.requestPulse(0, false);
            }
            if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intent.getAction())) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                DozeTriggers.this.mDozeSensors.onUserSwitched();
            }
        }

        public void register(Context context) {
            if (this.mRegistered) {
                return;
            }
            IntentFilter intentFilter = new IntentFilter("com.android.systemui.doze.pulse");
            intentFilter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            context.registerReceiver(this, intentFilter);
            this.mRegistered = true;
        }

        public void unregister(Context context) {
            if (!this.mRegistered) {
                return;
            }
            context.unregisterReceiver(this);
            this.mRegistered = false;
        }
    }
}
