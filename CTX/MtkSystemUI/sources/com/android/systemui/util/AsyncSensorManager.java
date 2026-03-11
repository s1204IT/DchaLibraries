package com.android.systemui.util;

import android.hardware.HardwareBuffer;
import android.hardware.Sensor;
import android.hardware.SensorAdditionalInfo;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.MemoryFile;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.util.List;

public class AsyncSensorManager extends SensorManager {

    @VisibleForTesting
    final Handler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread("async_sensor");
    private final SensorManager mInner;
    private final List<Sensor> mSensorCache;

    public AsyncSensorManager(SensorManager sensorManager) {
        this.mInner = sensorManager;
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mSensorCache = this.mInner.getSensorList(-1);
    }

    protected List<Sensor> getFullSensorList() {
        return this.mSensorCache;
    }

    protected List<Sensor> getFullDynamicSensorList() {
        return this.mInner.getDynamicSensorList(-1);
    }

    protected boolean registerListenerImpl(final SensorEventListener sensorEventListener, final Sensor sensor, final int i, final Handler handler, final int i2, int i3) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AsyncSensorManager.lambda$registerListenerImpl$0(this.f$0, sensorEventListener, sensor, i, i2, handler);
            }
        });
        return true;
    }

    public static void lambda$registerListenerImpl$0(AsyncSensorManager asyncSensorManager, SensorEventListener sensorEventListener, Sensor sensor, int i, int i2, Handler handler) {
        if (!asyncSensorManager.mInner.registerListener(sensorEventListener, sensor, i, i2, handler)) {
            Log.e("AsyncSensorManager", "Registering " + sensorEventListener + " for " + sensor + " failed.");
        }
    }

    protected boolean flushImpl(SensorEventListener sensorEventListener) {
        return this.mInner.flush(sensorEventListener);
    }

    protected SensorDirectChannel createDirectChannelImpl(MemoryFile memoryFile, HardwareBuffer hardwareBuffer) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void destroyDirectChannelImpl(SensorDirectChannel sensorDirectChannel) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected int configureDirectChannelImpl(SensorDirectChannel sensorDirectChannel, Sensor sensor, int i) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void registerDynamicSensorCallbackImpl(final SensorManager.DynamicSensorCallback dynamicSensorCallback, final Handler handler) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mInner.registerDynamicSensorCallback(dynamicSensorCallback, handler);
            }
        });
    }

    protected void unregisterDynamicSensorCallbackImpl(final SensorManager.DynamicSensorCallback dynamicSensorCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mInner.unregisterDynamicSensorCallback(dynamicSensorCallback);
            }
        });
    }

    protected boolean requestTriggerSensorImpl(final TriggerEventListener triggerEventListener, final Sensor sensor) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AsyncSensorManager.lambda$requestTriggerSensorImpl$3(this.f$0, triggerEventListener, sensor);
            }
        });
        return true;
    }

    public static void lambda$requestTriggerSensorImpl$3(AsyncSensorManager asyncSensorManager, TriggerEventListener triggerEventListener, Sensor sensor) {
        if (!asyncSensorManager.mInner.requestTriggerSensor(triggerEventListener, sensor)) {
            Log.e("AsyncSensorManager", "Requesting " + triggerEventListener + " for " + sensor + " failed.");
        }
    }

    protected boolean cancelTriggerSensorImpl(final TriggerEventListener triggerEventListener, final Sensor sensor, boolean z) {
        Preconditions.checkArgument(z);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AsyncSensorManager.lambda$cancelTriggerSensorImpl$4(this.f$0, triggerEventListener, sensor);
            }
        });
        return true;
    }

    public static void lambda$cancelTriggerSensorImpl$4(AsyncSensorManager asyncSensorManager, TriggerEventListener triggerEventListener, Sensor sensor) {
        if (!asyncSensorManager.mInner.cancelTriggerSensor(triggerEventListener, sensor)) {
            Log.e("AsyncSensorManager", "Canceling " + triggerEventListener + " for " + sensor + " failed.");
        }
    }

    protected boolean initDataInjectionImpl(boolean z) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected boolean injectSensorDataImpl(Sensor sensor, float[] fArr, int i, long j) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected boolean setOperationParameterImpl(final SensorAdditionalInfo sensorAdditionalInfo) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mInner.setOperationParameter(sensorAdditionalInfo);
            }
        });
        return true;
    }

    protected void unregisterListenerImpl(final SensorEventListener sensorEventListener, final Sensor sensor) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AsyncSensorManager.lambda$unregisterListenerImpl$6(this.f$0, sensor, sensorEventListener);
            }
        });
    }

    public static void lambda$unregisterListenerImpl$6(AsyncSensorManager asyncSensorManager, Sensor sensor, SensorEventListener sensorEventListener) {
        if (sensor == null) {
            asyncSensorManager.mInner.unregisterListener(sensorEventListener);
        } else {
            asyncSensorManager.mInner.unregisterListener(sensorEventListener, sensor);
        }
    }
}
