package android.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import dalvik.system.CloseGuard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SystemSensorManager extends SensorManager {
    private final Looper mMainLooper;
    private final int mTargetSdkLevel;
    private static boolean sSensorModuleInitialized = false;
    private static final Object sSensorModuleLock = new Object();
    private static final ArrayList<Sensor> sFullSensorsList = new ArrayList<>();
    private static final SparseArray<Sensor> sHandleToSensor = new SparseArray<>();
    private final HashMap<SensorEventListener, SensorEventQueue> mSensorListeners = new HashMap<>();
    private final HashMap<TriggerEventListener, TriggerEventQueue> mTriggerListeners = new HashMap<>();

    private static native void nativeClassInit();

    private static native int nativeGetNextSensor(Sensor sensor, int i);

    public SystemSensorManager(Context context, Looper mainLooper) {
        this.mMainLooper = mainLooper;
        this.mTargetSdkLevel = context.getApplicationInfo().targetSdkVersion;
        synchronized (sSensorModuleLock) {
            if (!sSensorModuleInitialized) {
                sSensorModuleInitialized = true;
                nativeClassInit();
                ArrayList<Sensor> fullList = sFullSensorsList;
                int i = 0;
                do {
                    Sensor sensor = new Sensor();
                    i = nativeGetNextSensor(sensor, i);
                    if (i >= 0) {
                        fullList.add(sensor);
                        sHandleToSensor.append(sensor.getHandle(), sensor);
                    }
                } while (i > 0);
            }
        }
    }

    @Override
    protected List<Sensor> getFullSensorList() {
        return sFullSensorsList;
    }

    @Override
    protected boolean registerListenerImpl(SensorEventListener listener, Sensor sensor, int delayUs, Handler handler, int maxBatchReportLatencyUs, int reservedFlags) {
        boolean zAddSensor = false;
        if (listener == null || sensor == null) {
            Log.e("SensorManager", "sensor or listener is null");
        } else if (sensor.getReportingMode() == 2) {
            Log.e("SensorManager", "Trigger Sensors should use the requestTriggerSensor.");
        } else if (maxBatchReportLatencyUs < 0 || delayUs < 0) {
            Log.e("SensorManager", "maxBatchReportLatencyUs and delayUs should be non-negative");
        } else {
            synchronized (this.mSensorListeners) {
                SensorEventQueue queue = this.mSensorListeners.get(listener);
                if (queue == null) {
                    Looper looper = handler != null ? handler.getLooper() : this.mMainLooper;
                    SensorEventQueue queue2 = new SensorEventQueue(listener, looper, this);
                    if (!queue2.addSensor(sensor, delayUs, maxBatchReportLatencyUs, reservedFlags)) {
                        queue2.dispose();
                    } else {
                        this.mSensorListeners.put(listener, queue2);
                        zAddSensor = true;
                    }
                } else {
                    zAddSensor = queue.addSensor(sensor, delayUs, maxBatchReportLatencyUs, reservedFlags);
                }
            }
        }
        return zAddSensor;
    }

    @Override
    protected void unregisterListenerImpl(SensorEventListener listener, Sensor sensor) {
        boolean result;
        if (sensor == null || sensor.getReportingMode() != 2) {
            synchronized (this.mSensorListeners) {
                SensorEventQueue queue = this.mSensorListeners.get(listener);
                if (queue != null) {
                    if (sensor == null) {
                        result = queue.removeAllSensors();
                    } else {
                        result = queue.removeSensor(sensor, true);
                    }
                    if (result && !queue.hasSensors()) {
                        this.mSensorListeners.remove(listener);
                        queue.dispose();
                    }
                }
            }
        }
    }

    @Override
    protected boolean requestTriggerSensorImpl(TriggerEventListener listener, Sensor sensor) {
        boolean zAddSensor = false;
        if (sensor == null) {
            throw new IllegalArgumentException("sensor cannot be null");
        }
        if (sensor.getReportingMode() == 2) {
            synchronized (this.mTriggerListeners) {
                TriggerEventQueue queue = this.mTriggerListeners.get(listener);
                if (queue == null) {
                    TriggerEventQueue queue2 = new TriggerEventQueue(listener, this.mMainLooper, this);
                    if (!queue2.addSensor(sensor, 0, 0, 0)) {
                        queue2.dispose();
                    } else {
                        this.mTriggerListeners.put(listener, queue2);
                        zAddSensor = true;
                    }
                } else {
                    zAddSensor = queue.addSensor(sensor, 0, 0, 0);
                }
            }
        }
        return zAddSensor;
    }

    @Override
    protected boolean cancelTriggerSensorImpl(TriggerEventListener listener, Sensor sensor, boolean disable) {
        boolean result = false;
        if (sensor == null || sensor.getReportingMode() == 2) {
            synchronized (this.mTriggerListeners) {
                TriggerEventQueue queue = this.mTriggerListeners.get(listener);
                if (queue != null) {
                    if (sensor == null) {
                        result = queue.removeAllSensors();
                    } else {
                        result = queue.removeSensor(sensor, disable);
                    }
                    if (result && !queue.hasSensors()) {
                        this.mTriggerListeners.remove(listener);
                        queue.dispose();
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected boolean flushImpl(SensorEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue queue = this.mSensorListeners.get(listener);
            if (queue != null) {
                z = queue.flush() == 0;
            }
        }
        return z;
    }

    private static abstract class BaseEventQueue {
        protected final SystemSensorManager mManager;
        private long nSensorEventQueue;
        private final SparseBooleanArray mActiveSensors = new SparseBooleanArray();
        protected final SparseIntArray mSensorAccuracies = new SparseIntArray();
        protected final SparseBooleanArray mFirstEvent = new SparseBooleanArray();
        private final CloseGuard mCloseGuard = CloseGuard.get();
        private final float[] mScratch = new float[16];

        private static native void nativeDestroySensorEventQueue(long j);

        private static native int nativeDisableSensor(long j, int i);

        private static native int nativeEnableSensor(long j, int i, int i2, int i3, int i4);

        private static native int nativeFlushSensor(long j);

        private native long nativeInitBaseEventQueue(BaseEventQueue baseEventQueue, MessageQueue messageQueue, float[] fArr);

        protected abstract void addSensorEvent(Sensor sensor);

        protected abstract void dispatchFlushCompleteEvent(int i);

        protected abstract void dispatchSensorEvent(int i, float[] fArr, int i2, long j);

        protected abstract void removeSensorEvent(Sensor sensor);

        BaseEventQueue(Looper looper, SystemSensorManager manager) {
            this.nSensorEventQueue = nativeInitBaseEventQueue(this, looper.getQueue(), this.mScratch);
            this.mCloseGuard.open("dispose");
            this.mManager = manager;
        }

        public void dispose() {
            dispose(false);
        }

        public boolean addSensor(Sensor sensor, int delayUs, int maxBatchReportLatencyUs, int reservedFlags) {
            int handle = sensor.getHandle();
            if (this.mActiveSensors.get(handle)) {
                return false;
            }
            this.mActiveSensors.put(handle, true);
            addSensorEvent(sensor);
            if (enableSensor(sensor, delayUs, maxBatchReportLatencyUs, reservedFlags) == 0 || (maxBatchReportLatencyUs != 0 && (maxBatchReportLatencyUs <= 0 || enableSensor(sensor, delayUs, 0, 0) == 0))) {
                return true;
            }
            removeSensor(sensor, false);
            return false;
        }

        public boolean removeAllSensors() {
            for (int i = 0; i < this.mActiveSensors.size(); i++) {
                if (this.mActiveSensors.valueAt(i)) {
                    int handle = this.mActiveSensors.keyAt(i);
                    Sensor sensor = (Sensor) SystemSensorManager.sHandleToSensor.get(handle);
                    if (sensor != null) {
                        disableSensor(sensor);
                        this.mActiveSensors.put(handle, false);
                        removeSensorEvent(sensor);
                    }
                }
            }
            return true;
        }

        public boolean removeSensor(Sensor sensor, boolean disable) {
            int handle = sensor.getHandle();
            if (!this.mActiveSensors.get(handle)) {
                return false;
            }
            if (disable) {
                disableSensor(sensor);
            }
            this.mActiveSensors.put(sensor.getHandle(), false);
            removeSensorEvent(sensor);
            return true;
        }

        public int flush() {
            if (this.nSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            return nativeFlushSensor(this.nSensorEventQueue);
        }

        public boolean hasSensors() {
            return this.mActiveSensors.indexOfValue(true) >= 0;
        }

        protected void finalize() throws Throwable {
            try {
                dispose(true);
            } finally {
                super.finalize();
            }
        }

        private void dispose(boolean finalized) {
            if (this.mCloseGuard != null) {
                if (finalized) {
                    this.mCloseGuard.warnIfOpen();
                }
                this.mCloseGuard.close();
            }
            if (this.nSensorEventQueue != 0) {
                nativeDestroySensorEventQueue(this.nSensorEventQueue);
                this.nSensorEventQueue = 0L;
            }
        }

        private int enableSensor(Sensor sensor, int rateUs, int maxBatchReportLatencyUs, int reservedFlags) {
            if (this.nSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            if (sensor == null) {
                throw new NullPointerException();
            }
            return nativeEnableSensor(this.nSensorEventQueue, sensor.getHandle(), rateUs, maxBatchReportLatencyUs, reservedFlags);
        }

        private int disableSensor(Sensor sensor) {
            if (this.nSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            if (sensor == null) {
                throw new NullPointerException();
            }
            return nativeDisableSensor(this.nSensorEventQueue, sensor.getHandle());
        }
    }

    static final class SensorEventQueue extends BaseEventQueue {
        private final SensorEventListener mListener;
        private final SparseArray<SensorEvent> mSensorsEvents;

        public SensorEventQueue(SensorEventListener listener, Looper looper, SystemSensorManager manager) {
            super(looper, manager);
            this.mSensorsEvents = new SparseArray<>();
            this.mListener = listener;
        }

        @Override
        public void addSensorEvent(Sensor sensor) {
            SensorEvent t = new SensorEvent(Sensor.getMaxLengthValuesArray(sensor, this.mManager.mTargetSdkLevel));
            synchronized (this.mSensorsEvents) {
                this.mSensorsEvents.put(sensor.getHandle(), t);
            }
        }

        @Override
        public void removeSensorEvent(Sensor sensor) {
            synchronized (this.mSensorsEvents) {
                this.mSensorsEvents.delete(sensor.getHandle());
            }
        }

        @Override
        protected void dispatchSensorEvent(int handle, float[] values, int inAccuracy, long timestamp) {
            SensorEvent t;
            Sensor sensor = (Sensor) SystemSensorManager.sHandleToSensor.get(handle);
            synchronized (this.mSensorsEvents) {
                t = this.mSensorsEvents.get(handle);
            }
            if (t != null) {
                System.arraycopy(values, 0, t.values, 0, t.values.length);
                t.timestamp = timestamp;
                t.accuracy = inAccuracy;
                t.sensor = sensor;
                int accuracy = this.mSensorAccuracies.get(handle);
                if (t.accuracy >= 0 && accuracy != t.accuracy) {
                    this.mSensorAccuracies.put(handle, t.accuracy);
                    this.mListener.onAccuracyChanged(t.sensor, t.accuracy);
                }
                this.mListener.onSensorChanged(t);
            }
        }

        @Override
        protected void dispatchFlushCompleteEvent(int handle) {
            if (this.mListener instanceof SensorEventListener2) {
                Sensor sensor = (Sensor) SystemSensorManager.sHandleToSensor.get(handle);
                ((SensorEventListener2) this.mListener).onFlushCompleted(sensor);
            }
        }
    }

    static final class TriggerEventQueue extends BaseEventQueue {
        private final TriggerEventListener mListener;
        private final SparseArray<TriggerEvent> mTriggerEvents;

        public TriggerEventQueue(TriggerEventListener listener, Looper looper, SystemSensorManager manager) {
            super(looper, manager);
            this.mTriggerEvents = new SparseArray<>();
            this.mListener = listener;
        }

        @Override
        public void addSensorEvent(Sensor sensor) {
            TriggerEvent t = new TriggerEvent(Sensor.getMaxLengthValuesArray(sensor, this.mManager.mTargetSdkLevel));
            synchronized (this.mTriggerEvents) {
                this.mTriggerEvents.put(sensor.getHandle(), t);
            }
        }

        @Override
        public void removeSensorEvent(Sensor sensor) {
            synchronized (this.mTriggerEvents) {
                this.mTriggerEvents.delete(sensor.getHandle());
            }
        }

        @Override
        protected void dispatchSensorEvent(int handle, float[] values, int accuracy, long timestamp) {
            TriggerEvent t;
            Sensor sensor = (Sensor) SystemSensorManager.sHandleToSensor.get(handle);
            synchronized (this.mTriggerEvents) {
                t = this.mTriggerEvents.get(handle);
            }
            if (t == null) {
                Log.e("SensorManager", "Error: Trigger Event is null for Sensor: " + sensor);
                return;
            }
            System.arraycopy(values, 0, t.values, 0, t.values.length);
            t.timestamp = timestamp;
            t.sensor = sensor;
            this.mManager.cancelTriggerSensorImpl(this.mListener, sensor, false);
            this.mListener.onTrigger(t);
        }

        @Override
        protected void dispatchFlushCompleteEvent(int handle) {
        }
    }
}
