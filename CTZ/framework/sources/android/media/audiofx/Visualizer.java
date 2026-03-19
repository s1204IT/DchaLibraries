package android.media.audiofx;

import android.app.ActivityThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;

public class Visualizer {
    public static final int ALREADY_EXISTS = -2;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -4;
    public static final int ERROR_DEAD_OBJECT = -7;
    public static final int ERROR_INVALID_OPERATION = -5;
    public static final int ERROR_NO_INIT = -3;
    public static final int ERROR_NO_MEMORY = -6;
    public static final int MEASUREMENT_MODE_NONE = 0;
    public static final int MEASUREMENT_MODE_PEAK_RMS = 1;
    private static final int NATIVE_EVENT_FFT_CAPTURE = 1;
    private static final int NATIVE_EVENT_PCM_CAPTURE = 0;
    private static final int NATIVE_EVENT_SERVER_DIED = 2;
    public static final int SCALING_MODE_AS_PLAYED = 1;
    public static final int SCALING_MODE_NORMALIZED = 0;
    public static final int STATE_ENABLED = 2;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int SUCCESS = 0;
    private static final String TAG = "Visualizer-JAVA";
    private int mId;
    private long mJniData;
    private long mNativeVisualizer;
    private int mState;
    private final Object mStateLock = new Object();
    private final Object mListenerLock = new Object();
    private NativeEventHandler mNativeEventHandler = null;
    private OnDataCaptureListener mCaptureListener = null;
    private OnServerDiedListener mServerDiedListener = null;

    public static final class MeasurementPeakRms {
        public int mPeak;
        public int mRms;
    }

    public interface OnDataCaptureListener {
        void onFftDataCapture(Visualizer visualizer, byte[] bArr, int i);

        void onWaveFormDataCapture(Visualizer visualizer, byte[] bArr, int i);
    }

    public interface OnServerDiedListener {
        void onServerDied();
    }

    public static native int[] getCaptureSizeRange();

    public static native int getMaxCaptureRate();

    private final native void native_finalize();

    private final native int native_getCaptureSize();

    private final native boolean native_getEnabled();

    private final native int native_getFft(byte[] bArr);

    private final native int native_getMeasurementMode();

    private final native int native_getPeakRms(MeasurementPeakRms measurementPeakRms);

    private final native int native_getSamplingRate();

    private final native int native_getScalingMode();

    private final native int native_getWaveForm(byte[] bArr);

    private static final native void native_init();

    private final native void native_release();

    private final native int native_setCaptureSize(int i);

    private final native int native_setEnabled(boolean z);

    private final native int native_setMeasurementMode(int i);

    private final native int native_setPeriodicCapture(int i, boolean z, boolean z2);

    private final native int native_setScalingMode(int i);

    private final native int native_setup(Object obj, int i, int[] iArr, String str);

    static {
        System.loadLibrary("audioeffect_jni");
        native_init();
    }

    public Visualizer(int i) throws RuntimeException {
        this.mState = 0;
        int[] iArr = new int[1];
        synchronized (this.mStateLock) {
            this.mState = 0;
            int iNative_setup = native_setup(new WeakReference(this), i, iArr, ActivityThread.currentOpPackageName());
            if (iNative_setup != 0 && iNative_setup != -2) {
                Log.e(TAG, "Error code " + iNative_setup + " when initializing Visualizer.");
                if (iNative_setup == -5) {
                    throw new UnsupportedOperationException("Effect library not loaded");
                }
                throw new RuntimeException("Cannot initialize Visualizer engine, error: " + iNative_setup);
            }
            this.mId = iArr[0];
            if (native_getEnabled()) {
                this.mState = 2;
            } else {
                this.mState = 1;
            }
        }
    }

    public void release() {
        synchronized (this.mStateLock) {
            native_release();
            this.mState = 0;
        }
    }

    protected void finalize() {
        native_finalize();
    }

    public int setEnabled(boolean z) throws IllegalStateException {
        int iNative_setEnabled;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("setEnabled() called in wrong state: " + this.mState);
            }
            iNative_setEnabled = 0;
            int i = 2;
            if (((z && this.mState == 1) || (!z && this.mState == 2)) && (iNative_setEnabled = native_setEnabled(z)) == 0) {
                if (!z) {
                    i = 1;
                }
                this.mState = i;
            }
        }
        return iNative_setEnabled;
    }

    public boolean getEnabled() {
        boolean zNative_getEnabled;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("getEnabled() called in wrong state: " + this.mState);
            }
            zNative_getEnabled = native_getEnabled();
        }
        return zNative_getEnabled;
    }

    public int setCaptureSize(int i) throws IllegalStateException {
        int iNative_setCaptureSize;
        synchronized (this.mStateLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("setCaptureSize() called in wrong state: " + this.mState);
            }
            iNative_setCaptureSize = native_setCaptureSize(i);
        }
        return iNative_setCaptureSize;
    }

    public int getCaptureSize() throws IllegalStateException {
        int iNative_getCaptureSize;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("getCaptureSize() called in wrong state: " + this.mState);
            }
            iNative_getCaptureSize = native_getCaptureSize();
        }
        return iNative_getCaptureSize;
    }

    public int setScalingMode(int i) throws IllegalStateException {
        int iNative_setScalingMode;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("setScalingMode() called in wrong state: " + this.mState);
            }
            iNative_setScalingMode = native_setScalingMode(i);
        }
        return iNative_setScalingMode;
    }

    public int getScalingMode() throws IllegalStateException {
        int iNative_getScalingMode;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("getScalingMode() called in wrong state: " + this.mState);
            }
            iNative_getScalingMode = native_getScalingMode();
        }
        return iNative_getScalingMode;
    }

    public int setMeasurementMode(int i) throws IllegalStateException {
        int iNative_setMeasurementMode;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("setMeasurementMode() called in wrong state: " + this.mState);
            }
            iNative_setMeasurementMode = native_setMeasurementMode(i);
        }
        return iNative_setMeasurementMode;
    }

    public int getMeasurementMode() throws IllegalStateException {
        int iNative_getMeasurementMode;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("getMeasurementMode() called in wrong state: " + this.mState);
            }
            iNative_getMeasurementMode = native_getMeasurementMode();
        }
        return iNative_getMeasurementMode;
    }

    public int getSamplingRate() throws IllegalStateException {
        int iNative_getSamplingRate;
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                throw new IllegalStateException("getSamplingRate() called in wrong state: " + this.mState);
            }
            iNative_getSamplingRate = native_getSamplingRate();
        }
        return iNative_getSamplingRate;
    }

    public int getWaveForm(byte[] bArr) throws IllegalStateException {
        int iNative_getWaveForm;
        synchronized (this.mStateLock) {
            if (this.mState != 2) {
                throw new IllegalStateException("getWaveForm() called in wrong state: " + this.mState);
            }
            iNative_getWaveForm = native_getWaveForm(bArr);
        }
        return iNative_getWaveForm;
    }

    public int getFft(byte[] bArr) throws IllegalStateException {
        int iNative_getFft;
        synchronized (this.mStateLock) {
            if (this.mState != 2) {
                throw new IllegalStateException("getFft() called in wrong state: " + this.mState);
            }
            iNative_getFft = native_getFft(bArr);
        }
        return iNative_getFft;
    }

    public int getMeasurementPeakRms(MeasurementPeakRms measurementPeakRms) {
        int iNative_getPeakRms;
        if (measurementPeakRms == null) {
            Log.e(TAG, "Cannot store measurements in a null object");
            return -4;
        }
        synchronized (this.mStateLock) {
            if (this.mState != 2) {
                throw new IllegalStateException("getMeasurementPeakRms() called in wrong state: " + this.mState);
            }
            iNative_getPeakRms = native_getPeakRms(measurementPeakRms);
        }
        return iNative_getPeakRms;
    }

    public int setDataCaptureListener(OnDataCaptureListener onDataCaptureListener, int i, boolean z, boolean z2) {
        synchronized (this.mListenerLock) {
            this.mCaptureListener = onDataCaptureListener;
        }
        if (onDataCaptureListener == null) {
            z = false;
            z2 = false;
        }
        int iNative_setPeriodicCapture = native_setPeriodicCapture(i, z, z2);
        if (iNative_setPeriodicCapture == 0 && onDataCaptureListener != null && this.mNativeEventHandler == null) {
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper != null) {
                this.mNativeEventHandler = new NativeEventHandler(this, looperMyLooper);
                return iNative_setPeriodicCapture;
            }
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                this.mNativeEventHandler = new NativeEventHandler(this, mainLooper);
                return iNative_setPeriodicCapture;
            }
            this.mNativeEventHandler = null;
            return -3;
        }
        return iNative_setPeriodicCapture;
    }

    public int setServerDiedListener(OnServerDiedListener onServerDiedListener) {
        synchronized (this.mListenerLock) {
            this.mServerDiedListener = onServerDiedListener;
        }
        return 0;
    }

    private class NativeEventHandler extends Handler {
        private Visualizer mVisualizer;

        public NativeEventHandler(Visualizer visualizer, Looper looper) {
            super(looper);
            this.mVisualizer = visualizer;
        }

        private void handleCaptureMessage(Message message) {
            OnDataCaptureListener onDataCaptureListener;
            synchronized (Visualizer.this.mListenerLock) {
                onDataCaptureListener = this.mVisualizer.mCaptureListener;
            }
            if (onDataCaptureListener != null) {
                byte[] bArr = (byte[]) message.obj;
                int i = message.arg1;
                switch (message.what) {
                    case 0:
                        onDataCaptureListener.onWaveFormDataCapture(this.mVisualizer, bArr, i);
                        return;
                    case 1:
                        onDataCaptureListener.onFftDataCapture(this.mVisualizer, bArr, i);
                        return;
                    default:
                        Log.e(Visualizer.TAG, "Unknown native event in handleCaptureMessge: " + message.what);
                        return;
                }
            }
        }

        private void handleServerDiedMessage(Message message) {
            OnServerDiedListener onServerDiedListener;
            synchronized (Visualizer.this.mListenerLock) {
                onServerDiedListener = this.mVisualizer.mServerDiedListener;
            }
            if (onServerDiedListener != null) {
                onServerDiedListener.onServerDied();
            }
        }

        @Override
        public void handleMessage(Message message) {
            if (this.mVisualizer == null) {
            }
            switch (message.what) {
                case 0:
                case 1:
                    handleCaptureMessage(message);
                    break;
                case 2:
                    handleServerDiedMessage(message);
                    break;
                default:
                    Log.e(Visualizer.TAG, "Unknown native event: " + message.what);
                    break;
            }
        }
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        Visualizer visualizer = (Visualizer) ((WeakReference) obj).get();
        if (visualizer != null && visualizer.mNativeEventHandler != null) {
            visualizer.mNativeEventHandler.sendMessage(visualizer.mNativeEventHandler.obtainMessage(i, i2, i3, obj2));
        }
    }
}
