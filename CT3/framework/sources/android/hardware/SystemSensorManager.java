package android.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.net.ProxyInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import dalvik.system.CloseGuard;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SystemSensorManager extends SensorManager {
    private final Context mContext;
    private BroadcastReceiver mDynamicSensorBroadcastReceiver;
    private final Looper mMainLooper;
    private final long mNativeInstance;
    private final int mTargetSdkLevel;
    private static boolean DEBUG_DYNAMIC_SENSOR = true;
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static boolean sNativeClassInited = false;

    @GuardedBy("sLock")
    private static InjectEventQueue sInjectEventQueue = null;
    private final ArrayList<Sensor> mFullSensorsList = new ArrayList<>();
    private List<Sensor> mFullDynamicSensorsList = new ArrayList();
    private boolean mDynamicSensorListDirty = true;
    private final HashMap<Integer, Sensor> mHandleToSensor = new HashMap<>();
    private final HashMap<SensorEventListener, SensorEventQueue> mSensorListeners = new HashMap<>();
    private final HashMap<TriggerEventListener, TriggerEventQueue> mTriggerListeners = new HashMap<>();
    private HashMap<SensorManager.DynamicSensorCallback, Handler> mDynamicSensorCallbacks = new HashMap<>();

    private static native void nativeClassInit();

    private static native long nativeCreate(String str);

    private static native void nativeGetDynamicSensors(long j, List<Sensor> list);

    private static native boolean nativeGetSensorAtIndex(long j, Sensor sensor, int i);

    private static native boolean nativeIsDataInjectionEnabled(long j);

    public SystemSensorManager(Context context, Looper mainLooper) {
        synchronized (sLock) {
            if (!sNativeClassInited) {
                sNativeClassInited = true;
                nativeClassInit();
            }
        }
        this.mMainLooper = mainLooper;
        this.mTargetSdkLevel = context.getApplicationInfo().targetSdkVersion;
        this.mContext = context;
        this.mNativeInstance = nativeCreate(context.getOpPackageName());
        int index = 0;
        while (true) {
            Sensor sensor = new Sensor();
            if (!nativeGetSensorAtIndex(this.mNativeInstance, sensor, index)) {
                return;
            }
            this.mFullSensorsList.add(sensor);
            this.mHandleToSensor.put(Integer.valueOf(sensor.getHandle()), sensor);
            index++;
        }
    }

    @Override
    protected List<Sensor> getFullSensorList() {
        return this.mFullSensorsList;
    }

    @Override
    protected List<Sensor> getFullDynamicSensorList() {
        setupDynamicSensorBroadcastReceiver();
        updateDynamicSensorList();
        return this.mFullDynamicSensorsList;
    }

    @Override
    protected boolean registerListenerImpl(SensorEventListener listener, Sensor sensor, int delayUs, Handler handler, int maxBatchReportLatencyUs, int reservedFlags) {
        String fullClassName;
        if (listener == null || sensor == null) {
            Log.e("SensorManager", "sensor or listener is null");
            return false;
        }
        if (sensor.getReportingMode() == 2) {
            Log.e("SensorManager", "Trigger Sensors should use the requestTriggerSensor.");
            return false;
        }
        if (maxBatchReportLatencyUs < 0 || delayUs < 0) {
            Log.e("SensorManager", "maxBatchReportLatencyUs and delayUs should be non-negative");
            return false;
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue queue = this.mSensorListeners.get(listener);
            if (queue == null) {
                Looper looper = handler != null ? handler.getLooper() : this.mMainLooper;
                if (listener.getClass().getEnclosingClass() != null) {
                    fullClassName = listener.getClass().getEnclosingClass().getName();
                } else {
                    fullClassName = listener.getClass().getName();
                }
                SensorEventQueue queue2 = new SensorEventQueue(listener, looper, this, fullClassName);
                if (!queue2.addSensor(sensor, delayUs, maxBatchReportLatencyUs)) {
                    queue2.dispose();
                    return false;
                }
                this.mSensorListeners.put(listener, queue2);
                return true;
            }
            return queue.addSensor(sensor, delayUs, maxBatchReportLatencyUs);
        }
    }

    @Override
    protected void unregisterListenerImpl(SensorEventListener listener, Sensor sensor) {
        boolean result;
        if (sensor != null && sensor.getReportingMode() == 2) {
            return;
        }
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

    @Override
    protected boolean requestTriggerSensorImpl(TriggerEventListener listener, Sensor sensor) {
        String fullClassName;
        if (sensor == null) {
            throw new IllegalArgumentException("sensor cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (sensor.getReportingMode() != 2) {
            return false;
        }
        synchronized (this.mTriggerListeners) {
            TriggerEventQueue queue = this.mTriggerListeners.get(listener);
            if (queue == null) {
                if (listener.getClass().getEnclosingClass() != null) {
                    fullClassName = listener.getClass().getEnclosingClass().getName();
                } else {
                    fullClassName = listener.getClass().getName();
                }
                TriggerEventQueue queue2 = new TriggerEventQueue(listener, this.mMainLooper, this, fullClassName);
                if (!queue2.addSensor(sensor, 0, 0)) {
                    queue2.dispose();
                    return false;
                }
                this.mTriggerListeners.put(listener, queue2);
                return true;
            }
            return queue.addSensor(sensor, 0, 0);
        }
    }

    @Override
    protected boolean cancelTriggerSensorImpl(TriggerEventListener listener, Sensor sensor, boolean disable) {
        boolean result;
        if (sensor != null && sensor.getReportingMode() != 2) {
            return false;
        }
        synchronized (this.mTriggerListeners) {
            TriggerEventQueue queue = this.mTriggerListeners.get(listener);
            if (queue == null) {
                return false;
            }
            if (sensor == null) {
                result = queue.removeAllSensors();
            } else {
                result = queue.removeSensor(sensor, disable);
            }
            if (result && !queue.hasSensors()) {
                this.mTriggerListeners.remove(listener);
                queue.dispose();
            }
            return result;
        }
    }

    @Override
    protected boolean flushImpl(SensorEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue queue = this.mSensorListeners.get(listener);
            if (queue == null) {
                return false;
            }
            return queue.flush() == 0;
        }
    }

    @Override
    protected boolean initDataInjectionImpl(boolean enable) {
        synchronized (sLock) {
            if (enable) {
                boolean isDataInjectionModeEnabled = nativeIsDataInjectionEnabled(this.mNativeInstance);
                if (!isDataInjectionModeEnabled) {
                    Log.e("SensorManager", "Data Injection mode not enabled");
                    return false;
                }
                if (sInjectEventQueue == null) {
                    sInjectEventQueue = new InjectEventQueue(this.mMainLooper, this, this.mContext.getPackageName());
                }
            } else if (sInjectEventQueue != null) {
                sInjectEventQueue.dispose();
                sInjectEventQueue = null;
            }
            return true;
        }
    }

    @Override
    protected boolean injectSensorDataImpl(Sensor sensor, float[] values, int accuracy, long timestamp) {
        synchronized (sLock) {
            if (sInjectEventQueue == null) {
                Log.e("SensorManager", "Data injection mode not activated before calling injectSensorData");
                return false;
            }
            int ret = sInjectEventQueue.injectSensorData(sensor.getHandle(), values, accuracy, timestamp);
            if (ret != 0) {
                sInjectEventQueue.dispose();
                sInjectEventQueue = null;
            }
            return ret == 0;
        }
    }

    private void cleanupSensorConnection(Sensor sensor) {
        Cloneable cloneable;
        this.mHandleToSensor.remove(Integer.valueOf(sensor.getHandle()));
        if (sensor.getReportingMode() == 2) {
            cloneable = this.mTriggerListeners;
            synchronized (cloneable) {
                for (TriggerEventListener l : this.mTriggerListeners.keySet()) {
                    if (DEBUG_DYNAMIC_SENSOR) {
                        Log.i("SensorManager", "removed trigger listener" + l.toString() + " due to sensor disconnection");
                    }
                    cancelTriggerSensorImpl(l, sensor, true);
                }
            }
        } else {
            cloneable = this.mSensorListeners;
            synchronized (cloneable) {
                for (SensorEventListener l2 : this.mSensorListeners.keySet()) {
                    if (DEBUG_DYNAMIC_SENSOR) {
                        Log.i("SensorManager", "removed event listener" + l2.toString() + " due to sensor disconnection");
                    }
                    unregisterListenerImpl(l2, sensor);
                }
            }
        }
    }

    private void updateDynamicSensorList() {
        Handler handler;
        synchronized (this.mFullDynamicSensorsList) {
            if (this.mDynamicSensorListDirty) {
                List<Sensor> list = new ArrayList<>();
                nativeGetDynamicSensors(this.mNativeInstance, list);
                List<Sensor> updatedList = new ArrayList<>();
                final List<Sensor> addedList = new ArrayList<>();
                final List<Sensor> removedList = new ArrayList<>();
                boolean changed = diffSortedSensorList(this.mFullDynamicSensorsList, list, updatedList, addedList, removedList);
                if (changed) {
                    if (DEBUG_DYNAMIC_SENSOR) {
                        Log.i("SensorManager", "DYNS dynamic sensor list cached should be updated");
                    }
                    this.mFullDynamicSensorsList = updatedList;
                    for (Sensor s : addedList) {
                        this.mHandleToSensor.put(Integer.valueOf(s.getHandle()), s);
                    }
                    Handler mainHandler = new Handler(this.mContext.getMainLooper());
                    for (Map.Entry<SensorManager.DynamicSensorCallback, Handler> entry : this.mDynamicSensorCallbacks.entrySet()) {
                        final SensorManager.DynamicSensorCallback callback = entry.getKey();
                        if (entry.getValue() == null) {
                            handler = mainHandler;
                        } else {
                            Handler handler2 = entry.getValue();
                            handler = handler2;
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                for (Sensor s2 : addedList) {
                                    callback.onDynamicSensorConnected(s2);
                                }
                                for (Sensor s3 : removedList) {
                                    callback.onDynamicSensorDisconnected(s3);
                                }
                            }
                        });
                    }
                    Iterator s$iterator = removedList.iterator();
                    while (s$iterator.hasNext()) {
                        cleanupSensorConnection((Sensor) s$iterator.next());
                    }
                }
                this.mDynamicSensorListDirty = false;
            }
        }
    }

    private void setupDynamicSensorBroadcastReceiver() {
        if (this.mDynamicSensorBroadcastReceiver != null) {
            return;
        }
        this.mDynamicSensorBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != Intent.ACTION_DYNAMIC_SENSOR_CHANGED) {
                    return;
                }
                if (SystemSensorManager.DEBUG_DYNAMIC_SENSOR) {
                    Log.i("SensorManager", "DYNS received DYNAMIC_SENSOR_CHANED broadcast");
                }
                SystemSensorManager.this.mDynamicSensorListDirty = true;
                SystemSensorManager.this.updateDynamicSensorList();
            }
        };
        IntentFilter filter = new IntentFilter("dynamic_sensor_change");
        filter.addAction(Intent.ACTION_DYNAMIC_SENSOR_CHANGED);
        this.mContext.registerReceiver(this.mDynamicSensorBroadcastReceiver, filter);
    }

    private void teardownDynamicSensorBroadcastReceiver() {
        this.mDynamicSensorCallbacks.clear();
        this.mContext.unregisterReceiver(this.mDynamicSensorBroadcastReceiver);
        this.mDynamicSensorBroadcastReceiver = null;
    }

    @Override
    protected void registerDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback callback, Handler handler) {
        if (DEBUG_DYNAMIC_SENSOR) {
            Log.i("SensorManager", "DYNS Register dynamic sensor callback");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (this.mDynamicSensorCallbacks.containsKey(callback)) {
            return;
        }
        setupDynamicSensorBroadcastReceiver();
        this.mDynamicSensorCallbacks.put(callback, handler);
    }

    @Override
    protected void unregisterDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback callback) {
        if (DEBUG_DYNAMIC_SENSOR) {
            Log.i("SensorManager", "Removing dynamic sensor listerner");
        }
        this.mDynamicSensorCallbacks.remove(callback);
    }

    private static boolean diffSortedSensorList(List<Sensor> oldList, List<Sensor> newList, List<Sensor> updated, List<Sensor> added, List<Sensor> removed) {
        boolean changed = false;
        int i = 0;
        int j = 0;
        while (true) {
            if (j < oldList.size() && (i >= newList.size() || newList.get(i).getHandle() > oldList.get(j).getHandle())) {
                changed = true;
                if (removed != null) {
                    removed.add(oldList.get(j));
                }
                j++;
            } else if (i < newList.size() && (j >= oldList.size() || newList.get(i).getHandle() < oldList.get(j).getHandle())) {
                changed = true;
                if (added != null) {
                    added.add(newList.get(i));
                }
                if (updated != null) {
                    updated.add(newList.get(i));
                }
                i++;
            } else {
                if (i >= newList.size() || j >= oldList.size() || newList.get(i).getHandle() != oldList.get(j).getHandle()) {
                    break;
                }
                if (updated != null) {
                    updated.add(oldList.get(j));
                }
                i++;
                j++;
            }
        }
        return changed;
    }

    private static abstract class BaseEventQueue {
        protected static final int OPERATING_MODE_DATA_INJECTION = 1;
        protected static final int OPERATING_MODE_NORMAL = 0;
        protected final SystemSensorManager mManager;
        private long nSensorEventQueue;
        private final SparseBooleanArray mActiveSensors = new SparseBooleanArray();
        protected final SparseIntArray mSensorAccuracies = new SparseIntArray();
        private final CloseGuard mCloseGuard = CloseGuard.get();

        private static native void nativeDestroySensorEventQueue(long j);

        private static native int nativeDisableSensor(long j, int i);

        private static native int nativeEnableSensor(long j, int i, int i2, int i3);

        private static native int nativeFlushSensor(long j);

        private static native long nativeInitBaseEventQueue(long j, WeakReference<BaseEventQueue> weakReference, MessageQueue messageQueue, String str, int i, String str2);

        private static native int nativeInjectSensorData(long j, int i, float[] fArr, int i2, long j2);

        protected abstract void addSensorEvent(Sensor sensor);

        protected abstract void dispatchFlushCompleteEvent(int i);

        protected abstract void dispatchSensorEvent(int i, float[] fArr, int i2, long j);

        protected abstract void removeSensorEvent(Sensor sensor);

        BaseEventQueue(Looper looper, SystemSensorManager manager, int mode, String packageName) {
            this.nSensorEventQueue = nativeInitBaseEventQueue(manager.mNativeInstance, new WeakReference(this), looper.getQueue(), packageName == null ? ProxyInfo.LOCAL_EXCL_LIST : packageName, mode, manager.mContext.getOpPackageName());
            this.mCloseGuard.open("dispose");
            this.mManager = manager;
        }

        public void dispose() {
            dispose(false);
        }

        public boolean addSensor(Sensor sensor, int delayUs, int maxBatchReportLatencyUs) {
            int handle = sensor.getHandle();
            if (this.mActiveSensors.get(handle)) {
                return false;
            }
            this.mActiveSensors.put(handle, true);
            addSensorEvent(sensor);
            if (enableSensor(sensor, delayUs, maxBatchReportLatencyUs) == 0 || (maxBatchReportLatencyUs != 0 && (maxBatchReportLatencyUs <= 0 || enableSensor(sensor, delayUs, 0) == 0))) {
                return true;
            }
            removeSensor(sensor, false);
            return false;
        }

        public boolean removeAllSensors() {
            for (int i = 0; i < this.mActiveSensors.size(); i++) {
                if (this.mActiveSensors.valueAt(i)) {
                    int handle = this.mActiveSensors.keyAt(i);
                    Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(handle));
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
            if (this.nSensorEventQueue == 0) {
                return;
            }
            nativeDestroySensorEventQueue(this.nSensorEventQueue);
            this.nSensorEventQueue = 0L;
        }

        private int enableSensor(Sensor sensor, int rateUs, int maxBatchReportLatencyUs) {
            if (this.nSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            if (sensor == null) {
                throw new NullPointerException();
            }
            return nativeEnableSensor(this.nSensorEventQueue, sensor.getHandle(), rateUs, maxBatchReportLatencyUs);
        }

        protected int injectSensorDataBase(int handle, float[] values, int accuracy, long timestamp) {
            return nativeInjectSensorData(this.nSensorEventQueue, handle, values, accuracy, timestamp);
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

        protected void dispatchAdditionalInfoEvent(int handle, int type, int serial, float[] floatValues, int[] intValues) {
        }
    }

    static final class SensorEventQueue extends BaseEventQueue {
        private final SensorEventListener mListener;
        private final SparseArray<SensorEvent> mSensorsEvents;

        public SensorEventQueue(SensorEventListener listener, Looper looper, SystemSensorManager manager, String packageName) {
            super(looper, manager, 0, packageName);
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
            Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(handle));
            if (sensor == null) {
                return;
            }
            synchronized (this.mSensorsEvents) {
                t = this.mSensorsEvents.get(handle);
            }
            if (t == null) {
                return;
            }
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

        @Override
        protected void dispatchFlushCompleteEvent(int handle) {
            Sensor sensor;
            if (!(this.mListener instanceof SensorEventListener2) || (sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(handle))) == null) {
                return;
            }
            ((SensorEventListener2) this.mListener).onFlushCompleted(sensor);
        }

        @Override
        protected void dispatchAdditionalInfoEvent(int handle, int type, int serial, float[] floatValues, int[] intValues) {
            Sensor sensor;
            if (!(this.mListener instanceof SensorEventCallback) || (sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(handle))) == null) {
                return;
            }
            SensorAdditionalInfo info = new SensorAdditionalInfo(sensor, type, serial, intValues, floatValues);
            ((SensorEventCallback) this.mListener).onSensorAdditionalInfo(info);
        }
    }

    static final class TriggerEventQueue extends BaseEventQueue {
        private final TriggerEventListener mListener;
        private final SparseArray<TriggerEvent> mTriggerEvents;

        public TriggerEventQueue(TriggerEventListener listener, Looper looper, SystemSensorManager manager, String packageName) {
            super(looper, manager, 0, packageName);
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
            Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(handle));
            if (sensor == null) {
                return;
            }
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

    final class InjectEventQueue extends BaseEventQueue {
        public InjectEventQueue(Looper looper, SystemSensorManager manager, String packageName) {
            super(looper, manager, 1, packageName);
        }

        int injectSensorData(int handle, float[] values, int accuracy, long timestamp) {
            return injectSensorDataBase(handle, values, accuracy, timestamp);
        }

        @Override
        protected void dispatchSensorEvent(int handle, float[] values, int accuracy, long timestamp) {
        }

        @Override
        protected void dispatchFlushCompleteEvent(int handle) {
        }

        @Override
        protected void addSensorEvent(Sensor sensor) {
        }

        @Override
        protected void removeSensorEvent(Sensor sensor) {
        }
    }
}
