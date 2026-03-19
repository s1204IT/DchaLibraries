package android.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.MemoryFile;
import android.os.MessageQueue;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SystemSensorManager extends SensorManager {
    private static final boolean DEBUG_DYNAMIC_SENSOR = true;
    private static final int MAX_LISTENER_COUNT = 128;
    private static final int MIN_DIRECT_CHANNEL_BUFFER_SIZE = 104;
    private final Context mContext;
    private BroadcastReceiver mDynamicSensorBroadcastReceiver;
    private final Looper mMainLooper;
    private final long mNativeInstance;
    private final int mTargetSdkLevel;
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

    private static native int nativeConfigDirectChannel(long j, int i, int i2, int i3);

    private static native long nativeCreate(String str);

    private static native int nativeCreateDirectChannel(long j, long j2, int i, int i2, HardwareBuffer hardwareBuffer);

    private static native void nativeDestroyDirectChannel(long j, int i);

    private static native void nativeGetDynamicSensors(long j, List<Sensor> list);

    private static native boolean nativeGetSensorAtIndex(long j, Sensor sensor, int i);

    private static native boolean nativeIsDataInjectionEnabled(long j);

    private static native int nativeSetOperationParameter(long j, int i, int i2, float[] fArr, int[] iArr);

    public SystemSensorManager(Context context, Looper looper) {
        synchronized (sLock) {
            if (!sNativeClassInited) {
                sNativeClassInited = true;
                nativeClassInit();
            }
        }
        this.mMainLooper = looper;
        this.mTargetSdkLevel = context.getApplicationInfo().targetSdkVersion;
        this.mContext = context;
        this.mNativeInstance = nativeCreate(context.getOpPackageName());
        int i = 0;
        while (true) {
            Sensor sensor = new Sensor();
            if (nativeGetSensorAtIndex(this.mNativeInstance, sensor, i)) {
                this.mFullSensorsList.add(sensor);
                this.mHandleToSensor.put(Integer.valueOf(sensor.getHandle()), sensor);
                i++;
            } else {
                return;
            }
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
    protected boolean registerListenerImpl(SensorEventListener sensorEventListener, Sensor sensor, int i, Handler handler, int i2, int i3) {
        String name;
        if (sensorEventListener == null || sensor == null) {
            Log.e("SensorManager", "sensor or listener is null");
            return false;
        }
        if (sensor.getReportingMode() == 2) {
            Log.e("SensorManager", "Trigger Sensors should use the requestTriggerSensor.");
            return false;
        }
        if (i2 < 0 || i < 0) {
            Log.e("SensorManager", "maxBatchReportLatencyUs and delayUs should be non-negative");
            return false;
        }
        if (this.mSensorListeners.size() >= 128) {
            throw new IllegalStateException("register failed, the sensor listeners size has exceeded the maximum limit 128");
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue sensorEventQueue = this.mSensorListeners.get(sensorEventListener);
            if (sensorEventQueue == null) {
                Looper looper = handler != null ? handler.getLooper() : this.mMainLooper;
                if (sensorEventListener.getClass().getEnclosingClass() != null) {
                    name = sensorEventListener.getClass().getEnclosingClass().getName();
                } else {
                    name = sensorEventListener.getClass().getName();
                }
                SensorEventQueue sensorEventQueue2 = new SensorEventQueue(sensorEventListener, looper, this, name);
                if (!sensorEventQueue2.addSensor(sensor, i, i2)) {
                    sensorEventQueue2.dispose();
                    return false;
                }
                this.mSensorListeners.put(sensorEventListener, sensorEventQueue2);
                return true;
            }
            return sensorEventQueue.addSensor(sensor, i, i2);
        }
    }

    @Override
    protected void unregisterListenerImpl(SensorEventListener sensorEventListener, Sensor sensor) {
        boolean zRemoveSensor;
        if (sensor != null && sensor.getReportingMode() == 2) {
            return;
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue sensorEventQueue = this.mSensorListeners.get(sensorEventListener);
            if (sensorEventQueue != null) {
                if (sensor == null) {
                    zRemoveSensor = sensorEventQueue.removeAllSensors();
                } else {
                    zRemoveSensor = sensorEventQueue.removeSensor(sensor, true);
                }
                if (zRemoveSensor && !sensorEventQueue.hasSensors()) {
                    this.mSensorListeners.remove(sensorEventListener);
                    sensorEventQueue.dispose();
                }
            }
        }
    }

    @Override
    protected boolean requestTriggerSensorImpl(TriggerEventListener triggerEventListener, Sensor sensor) {
        String name;
        if (sensor == null) {
            throw new IllegalArgumentException("sensor cannot be null");
        }
        if (triggerEventListener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (sensor.getReportingMode() != 2) {
            return false;
        }
        if (this.mTriggerListeners.size() >= 128) {
            throw new IllegalStateException("request failed, the trigger listeners size has exceeded the maximum limit 128");
        }
        synchronized (this.mTriggerListeners) {
            TriggerEventQueue triggerEventQueue = this.mTriggerListeners.get(triggerEventListener);
            if (triggerEventQueue == null) {
                if (triggerEventListener.getClass().getEnclosingClass() != null) {
                    name = triggerEventListener.getClass().getEnclosingClass().getName();
                } else {
                    name = triggerEventListener.getClass().getName();
                }
                TriggerEventQueue triggerEventQueue2 = new TriggerEventQueue(triggerEventListener, this.mMainLooper, this, name);
                if (!triggerEventQueue2.addSensor(sensor, 0, 0)) {
                    triggerEventQueue2.dispose();
                    return false;
                }
                this.mTriggerListeners.put(triggerEventListener, triggerEventQueue2);
                return true;
            }
            return triggerEventQueue.addSensor(sensor, 0, 0);
        }
    }

    @Override
    protected boolean cancelTriggerSensorImpl(TriggerEventListener triggerEventListener, Sensor sensor, boolean z) {
        boolean zRemoveSensor;
        if (sensor != null && sensor.getReportingMode() != 2) {
            return false;
        }
        synchronized (this.mTriggerListeners) {
            TriggerEventQueue triggerEventQueue = this.mTriggerListeners.get(triggerEventListener);
            if (triggerEventQueue == null) {
                return false;
            }
            if (sensor == null) {
                zRemoveSensor = triggerEventQueue.removeAllSensors();
            } else {
                zRemoveSensor = triggerEventQueue.removeSensor(sensor, z);
            }
            if (zRemoveSensor && !triggerEventQueue.hasSensors()) {
                this.mTriggerListeners.remove(triggerEventListener);
                triggerEventQueue.dispose();
            }
            return zRemoveSensor;
        }
    }

    @Override
    protected boolean flushImpl(SensorEventListener sensorEventListener) {
        if (sensorEventListener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        synchronized (this.mSensorListeners) {
            SensorEventQueue sensorEventQueue = this.mSensorListeners.get(sensorEventListener);
            if (sensorEventQueue == null) {
                return false;
            }
            return sensorEventQueue.flush() == 0;
        }
    }

    @Override
    protected boolean initDataInjectionImpl(boolean z) {
        synchronized (sLock) {
            boolean z2 = true;
            try {
                if (z) {
                    if (!nativeIsDataInjectionEnabled(this.mNativeInstance)) {
                        Log.e("SensorManager", "Data Injection mode not enabled");
                        return false;
                    }
                    if (sInjectEventQueue == null) {
                        try {
                            sInjectEventQueue = new InjectEventQueue(this.mMainLooper, this, this.mContext.getPackageName());
                        } catch (RuntimeException e) {
                            Log.e("SensorManager", "Cannot create InjectEventQueue: " + e);
                        }
                    }
                    if (sInjectEventQueue == null) {
                        z2 = false;
                    }
                    return z2;
                }
                if (sInjectEventQueue != null) {
                    sInjectEventQueue.dispose();
                    sInjectEventQueue = null;
                }
                return true;
            } finally {
            }
        }
    }

    @Override
    protected boolean injectSensorDataImpl(Sensor sensor, float[] fArr, int i, long j) {
        synchronized (sLock) {
            if (sInjectEventQueue == null) {
                Log.e("SensorManager", "Data injection mode not activated before calling injectSensorData");
                return false;
            }
            int iInjectSensorData = sInjectEventQueue.injectSensorData(sensor.getHandle(), fArr, i, j);
            if (iInjectSensorData != 0) {
                sInjectEventQueue.dispose();
                sInjectEventQueue = null;
            }
            return iInjectSensorData == 0;
        }
    }

    private void cleanupSensorConnection(Sensor sensor) {
        this.mHandleToSensor.remove(Integer.valueOf(sensor.getHandle()));
        if (sensor.getReportingMode() == 2) {
            synchronized (this.mTriggerListeners) {
                for (TriggerEventListener triggerEventListener : new HashMap(this.mTriggerListeners).keySet()) {
                    Log.i("SensorManager", "removed trigger listener" + triggerEventListener.toString() + " due to sensor disconnection");
                    cancelTriggerSensorImpl(triggerEventListener, sensor, true);
                }
            }
            return;
        }
        synchronized (this.mSensorListeners) {
            for (SensorEventListener sensorEventListener : new HashMap(this.mSensorListeners).keySet()) {
                Log.i("SensorManager", "removed event listener" + sensorEventListener.toString() + " due to sensor disconnection");
                unregisterListenerImpl(sensorEventListener, sensor);
            }
        }
    }

    private void updateDynamicSensorList() {
        Handler value;
        synchronized (this.mFullDynamicSensorsList) {
            if (this.mDynamicSensorListDirty) {
                ArrayList arrayList = new ArrayList();
                nativeGetDynamicSensors(this.mNativeInstance, arrayList);
                ArrayList arrayList2 = new ArrayList();
                final ArrayList<Sensor> arrayList3 = new ArrayList();
                final ArrayList arrayList4 = new ArrayList();
                if (diffSortedSensorList(this.mFullDynamicSensorsList, arrayList, arrayList2, arrayList3, arrayList4)) {
                    Log.i("SensorManager", "DYNS dynamic sensor list cached should be updated");
                    this.mFullDynamicSensorsList = arrayList2;
                    for (Sensor sensor : arrayList3) {
                        this.mHandleToSensor.put(Integer.valueOf(sensor.getHandle()), sensor);
                    }
                    Handler handler = new Handler(this.mContext.getMainLooper());
                    for (Map.Entry<SensorManager.DynamicSensorCallback, Handler> entry : this.mDynamicSensorCallbacks.entrySet()) {
                        final SensorManager.DynamicSensorCallback key = entry.getKey();
                        if (entry.getValue() != null) {
                            value = entry.getValue();
                        } else {
                            value = handler;
                        }
                        value.post(new Runnable() {
                            @Override
                            public void run() {
                                Iterator it = arrayList3.iterator();
                                while (it.hasNext()) {
                                    key.onDynamicSensorConnected((Sensor) it.next());
                                }
                                Iterator it2 = arrayList4.iterator();
                                while (it2.hasNext()) {
                                    key.onDynamicSensorDisconnected((Sensor) it2.next());
                                }
                            }
                        });
                    }
                    Iterator it = arrayList4.iterator();
                    while (it.hasNext()) {
                        cleanupSensorConnection((Sensor) it.next());
                    }
                }
                this.mDynamicSensorListDirty = false;
            }
        }
    }

    private void setupDynamicSensorBroadcastReceiver() {
        if (this.mDynamicSensorBroadcastReceiver == null) {
            this.mDynamicSensorBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() == Intent.ACTION_DYNAMIC_SENSOR_CHANGED) {
                        Log.i("SensorManager", "DYNS received DYNAMIC_SENSOR_CHANED broadcast");
                        SystemSensorManager.this.mDynamicSensorListDirty = true;
                        SystemSensorManager.this.updateDynamicSensorList();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter("dynamic_sensor_change");
            intentFilter.addAction(Intent.ACTION_DYNAMIC_SENSOR_CHANGED);
            this.mContext.registerReceiver(this.mDynamicSensorBroadcastReceiver, intentFilter);
        }
    }

    private void teardownDynamicSensorBroadcastReceiver() {
        this.mDynamicSensorCallbacks.clear();
        this.mContext.unregisterReceiver(this.mDynamicSensorBroadcastReceiver);
        this.mDynamicSensorBroadcastReceiver = null;
    }

    @Override
    protected void registerDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback dynamicSensorCallback, Handler handler) {
        Log.i("SensorManager", "DYNS Register dynamic sensor callback");
        if (dynamicSensorCallback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (this.mDynamicSensorCallbacks.containsKey(dynamicSensorCallback)) {
            return;
        }
        setupDynamicSensorBroadcastReceiver();
        this.mDynamicSensorCallbacks.put(dynamicSensorCallback, handler);
    }

    @Override
    protected void unregisterDynamicSensorCallbackImpl(SensorManager.DynamicSensorCallback dynamicSensorCallback) {
        Log.i("SensorManager", "Removing dynamic sensor listerner");
        this.mDynamicSensorCallbacks.remove(dynamicSensorCallback);
    }

    private static boolean diffSortedSensorList(List<Sensor> list, List<Sensor> list2, List<Sensor> list3, List<Sensor> list4, List<Sensor> list5) {
        int i = 0;
        int i2 = 0;
        boolean z = false;
        while (true) {
            if (i < list.size() && (i2 >= list2.size() || list2.get(i2).getHandle() > list.get(i).getHandle())) {
                if (list5 != null) {
                    list5.add(list.get(i));
                }
                i++;
                z = true;
            } else if (i2 < list2.size() && (i >= list.size() || list2.get(i2).getHandle() < list.get(i).getHandle())) {
                if (list4 != null) {
                    list4.add(list2.get(i2));
                }
                if (list3 != null) {
                    list3.add(list2.get(i2));
                }
                i2++;
                z = true;
            } else {
                if (i2 >= list2.size() || i >= list.size() || list2.get(i2).getHandle() != list.get(i).getHandle()) {
                    break;
                }
                if (list3 != null) {
                    list3.add(list.get(i));
                }
                i2++;
                i++;
            }
        }
        return z;
    }

    @Override
    protected int configureDirectChannelImpl(SensorDirectChannel sensorDirectChannel, Sensor sensor, int i) {
        if (!sensorDirectChannel.isOpen()) {
            throw new IllegalStateException("channel is closed");
        }
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("rate parameter invalid");
        }
        if (sensor == null && i != 0) {
            throw new IllegalArgumentException("when sensor is null, rate can only be DIRECT_RATE_STOP");
        }
        int iNativeConfigDirectChannel = nativeConfigDirectChannel(this.mNativeInstance, sensorDirectChannel.getNativeHandle(), sensor == null ? -1 : sensor.getHandle(), i);
        if (i == 0) {
            return iNativeConfigDirectChannel == 0 ? 1 : 0;
        }
        if (iNativeConfigDirectChannel > 0) {
            return iNativeConfigDirectChannel;
        }
        return 0;
    }

    @Override
    protected SensorDirectChannel createDirectChannelImpl(MemoryFile memoryFile, HardwareBuffer hardwareBuffer) {
        long j;
        int i;
        int i2;
        if (memoryFile != null) {
            try {
                int int$ = memoryFile.getFileDescriptor().getInt$();
                if (memoryFile.length() < 104) {
                    throw new IllegalArgumentException("Size of MemoryFile has to be greater than 104");
                }
                long length = memoryFile.length();
                int iNativeCreateDirectChannel = nativeCreateDirectChannel(this.mNativeInstance, length, 1, int$, null);
                if (iNativeCreateDirectChannel <= 0) {
                    throw new UncheckedIOException(new IOException("create MemoryFile direct channel failed " + iNativeCreateDirectChannel));
                }
                j = length;
                i = iNativeCreateDirectChannel;
                i2 = 1;
            } catch (IOException e) {
                throw new IllegalArgumentException("MemoryFile object is not valid");
            }
        } else if (hardwareBuffer != null) {
            if (hardwareBuffer.getFormat() == 33) {
                if (hardwareBuffer.getHeight() != 1) {
                    throw new IllegalArgumentException("Height of HardwareBuffer must be 1");
                }
                if (hardwareBuffer.getWidth() < 104) {
                    throw new IllegalArgumentException("Width if HaradwareBuffer must be greater than 104");
                }
                if ((hardwareBuffer.getUsage() & 8388608) == 0) {
                    throw new IllegalArgumentException("HardwareBuffer must set usage flag USAGE_SENSOR_DIRECT_DATA");
                }
                long width = hardwareBuffer.getWidth();
                int iNativeCreateDirectChannel2 = nativeCreateDirectChannel(this.mNativeInstance, width, 2, -1, hardwareBuffer);
                if (iNativeCreateDirectChannel2 <= 0) {
                    throw new UncheckedIOException(new IOException("create HardwareBuffer direct channel failed " + iNativeCreateDirectChannel2));
                }
                i = iNativeCreateDirectChannel2;
                i2 = 2;
                j = width;
            } else {
                throw new IllegalArgumentException("Format of HardwareBuffer must be BLOB");
            }
        } else {
            throw new NullPointerException("shared memory object cannot be null");
        }
        return new SensorDirectChannel(this, i, i2, j);
    }

    @Override
    protected void destroyDirectChannelImpl(SensorDirectChannel sensorDirectChannel) {
        if (sensorDirectChannel != null) {
            nativeDestroyDirectChannel(this.mNativeInstance, sensorDirectChannel.getNativeHandle());
        }
    }

    private static abstract class BaseEventQueue {
        protected static final int OPERATING_MODE_DATA_INJECTION = 1;
        protected static final int OPERATING_MODE_NORMAL = 0;
        protected final SystemSensorManager mManager;
        private long mNativeSensorEventQueue;
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

        BaseEventQueue(Looper looper, SystemSensorManager systemSensorManager, int i, String str) {
            this.mNativeSensorEventQueue = nativeInitBaseEventQueue(systemSensorManager.mNativeInstance, new WeakReference(this), looper.getQueue(), str == null ? "" : str, i, systemSensorManager.mContext.getOpPackageName());
            this.mCloseGuard.open("dispose");
            this.mManager = systemSensorManager;
        }

        public void dispose() {
            dispose(false);
        }

        public boolean addSensor(Sensor sensor, int i, int i2) {
            int handle = sensor.getHandle();
            if (this.mActiveSensors.get(handle)) {
                return false;
            }
            this.mActiveSensors.put(handle, true);
            addSensorEvent(sensor);
            if (enableSensor(sensor, i, i2) == 0 || (i2 != 0 && (i2 <= 0 || enableSensor(sensor, i, 0) == 0))) {
                return true;
            }
            removeSensor(sensor, false);
            return false;
        }

        public boolean removeAllSensors() {
            for (int i = 0; i < this.mActiveSensors.size(); i++) {
                if (this.mActiveSensors.valueAt(i)) {
                    int iKeyAt = this.mActiveSensors.keyAt(i);
                    Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(iKeyAt));
                    if (sensor != null) {
                        disableSensor(sensor);
                        this.mActiveSensors.put(iKeyAt, false);
                        removeSensorEvent(sensor);
                    }
                }
            }
            return true;
        }

        public boolean removeSensor(Sensor sensor, boolean z) {
            if (!this.mActiveSensors.get(sensor.getHandle())) {
                return false;
            }
            if (z) {
                disableSensor(sensor);
            }
            this.mActiveSensors.put(sensor.getHandle(), false);
            removeSensorEvent(sensor);
            return true;
        }

        public int flush() {
            if (this.mNativeSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            return nativeFlushSensor(this.mNativeSensorEventQueue);
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

        private void dispose(boolean z) {
            if (this.mCloseGuard != null) {
                if (z) {
                    this.mCloseGuard.warnIfOpen();
                }
                this.mCloseGuard.close();
            }
            if (this.mNativeSensorEventQueue != 0) {
                nativeDestroySensorEventQueue(this.mNativeSensorEventQueue);
                this.mNativeSensorEventQueue = 0L;
            }
        }

        private int enableSensor(Sensor sensor, int i, int i2) {
            if (this.mNativeSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            if (sensor == null) {
                throw new NullPointerException();
            }
            return nativeEnableSensor(this.mNativeSensorEventQueue, sensor.getHandle(), i, i2);
        }

        protected int injectSensorDataBase(int i, float[] fArr, int i2, long j) {
            return nativeInjectSensorData(this.mNativeSensorEventQueue, i, fArr, i2, j);
        }

        private int disableSensor(Sensor sensor) {
            if (this.mNativeSensorEventQueue == 0) {
                throw new NullPointerException();
            }
            if (sensor == null) {
                throw new NullPointerException();
            }
            return nativeDisableSensor(this.mNativeSensorEventQueue, sensor.getHandle());
        }

        protected void dispatchAdditionalInfoEvent(int i, int i2, int i3, float[] fArr, int[] iArr) {
        }
    }

    static final class SensorEventQueue extends BaseEventQueue {
        private final SensorEventListener mListener;
        private final SparseArray<SensorEvent> mSensorsEvents;

        public SensorEventQueue(SensorEventListener sensorEventListener, Looper looper, SystemSensorManager systemSensorManager, String str) {
            super(looper, systemSensorManager, 0, str);
            this.mSensorsEvents = new SparseArray<>();
            this.mListener = sensorEventListener;
        }

        @Override
        public void addSensorEvent(Sensor sensor) {
            SensorEvent sensorEvent = new SensorEvent(Sensor.getMaxLengthValuesArray(sensor, this.mManager.mTargetSdkLevel));
            synchronized (this.mSensorsEvents) {
                this.mSensorsEvents.put(sensor.getHandle(), sensorEvent);
            }
        }

        @Override
        public void removeSensorEvent(Sensor sensor) {
            synchronized (this.mSensorsEvents) {
                this.mSensorsEvents.delete(sensor.getHandle());
            }
        }

        @Override
        protected void dispatchSensorEvent(int i, float[] fArr, int i2, long j) {
            SensorEvent sensorEvent;
            Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(i));
            if (sensor == null) {
                return;
            }
            synchronized (this.mSensorsEvents) {
                sensorEvent = this.mSensorsEvents.get(i);
            }
            if (sensorEvent == null) {
                return;
            }
            System.arraycopy(fArr, 0, sensorEvent.values, 0, sensorEvent.values.length);
            sensorEvent.timestamp = j;
            sensorEvent.accuracy = i2;
            sensorEvent.sensor = sensor;
            int i3 = this.mSensorAccuracies.get(i);
            if (sensorEvent.accuracy >= 0 && i3 != sensorEvent.accuracy) {
                this.mSensorAccuracies.put(i, sensorEvent.accuracy);
                this.mListener.onAccuracyChanged(sensorEvent.sensor, sensorEvent.accuracy);
            }
            this.mListener.onSensorChanged(sensorEvent);
        }

        @Override
        protected void dispatchFlushCompleteEvent(int i) {
            Sensor sensor;
            if (!(this.mListener instanceof SensorEventListener2) || (sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(i))) == null) {
                return;
            }
            ((SensorEventListener2) this.mListener).onFlushCompleted(sensor);
        }

        @Override
        protected void dispatchAdditionalInfoEvent(int i, int i2, int i3, float[] fArr, int[] iArr) {
            Sensor sensor;
            if (!(this.mListener instanceof SensorEventCallback) || (sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(i))) == null) {
                return;
            }
            ((SensorEventCallback) this.mListener).onSensorAdditionalInfo(new SensorAdditionalInfo(sensor, i2, i3, iArr, fArr));
        }
    }

    static final class TriggerEventQueue extends BaseEventQueue {
        private final TriggerEventListener mListener;
        private final SparseArray<TriggerEvent> mTriggerEvents;

        public TriggerEventQueue(TriggerEventListener triggerEventListener, Looper looper, SystemSensorManager systemSensorManager, String str) {
            super(looper, systemSensorManager, 0, str);
            this.mTriggerEvents = new SparseArray<>();
            this.mListener = triggerEventListener;
        }

        @Override
        public void addSensorEvent(Sensor sensor) {
            TriggerEvent triggerEvent = new TriggerEvent(Sensor.getMaxLengthValuesArray(sensor, this.mManager.mTargetSdkLevel));
            synchronized (this.mTriggerEvents) {
                this.mTriggerEvents.put(sensor.getHandle(), triggerEvent);
            }
        }

        @Override
        public void removeSensorEvent(Sensor sensor) {
            synchronized (this.mTriggerEvents) {
                this.mTriggerEvents.delete(sensor.getHandle());
            }
        }

        @Override
        protected void dispatchSensorEvent(int i, float[] fArr, int i2, long j) {
            TriggerEvent triggerEvent;
            Sensor sensor = (Sensor) this.mManager.mHandleToSensor.get(Integer.valueOf(i));
            if (sensor == null) {
                return;
            }
            synchronized (this.mTriggerEvents) {
                triggerEvent = this.mTriggerEvents.get(i);
            }
            if (triggerEvent == null) {
                Log.e("SensorManager", "Error: Trigger Event is null for Sensor: " + sensor);
                return;
            }
            System.arraycopy(fArr, 0, triggerEvent.values, 0, triggerEvent.values.length);
            triggerEvent.timestamp = j;
            triggerEvent.sensor = sensor;
            this.mManager.cancelTriggerSensorImpl(this.mListener, sensor, false);
            this.mListener.onTrigger(triggerEvent);
        }

        @Override
        protected void dispatchFlushCompleteEvent(int i) {
        }
    }

    final class InjectEventQueue extends BaseEventQueue {
        public InjectEventQueue(Looper looper, SystemSensorManager systemSensorManager, String str) {
            super(looper, systemSensorManager, 1, str);
        }

        int injectSensorData(int i, float[] fArr, int i2, long j) {
            return injectSensorDataBase(i, fArr, i2, j);
        }

        @Override
        protected void dispatchSensorEvent(int i, float[] fArr, int i2, long j) {
        }

        @Override
        protected void dispatchFlushCompleteEvent(int i) {
        }

        @Override
        protected void addSensorEvent(Sensor sensor) {
        }

        @Override
        protected void removeSensorEvent(Sensor sensor) {
        }
    }

    @Override
    protected boolean setOperationParameterImpl(SensorAdditionalInfo sensorAdditionalInfo) {
        return nativeSetOperationParameter(this.mNativeInstance, sensorAdditionalInfo.sensor != null ? sensorAdditionalInfo.sensor.getHandle() : -1, sensorAdditionalInfo.type, sensorAdditionalInfo.floatValues, sensorAdditionalInfo.intValues) == 0;
    }
}
