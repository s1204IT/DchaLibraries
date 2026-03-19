package com.android.server.tv;

import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import java.util.LinkedList;
import java.util.Queue;

final class TvInputHal implements Handler.Callback {
    private static final boolean DEBUG = false;
    public static final int ERROR_NO_INIT = -1;
    public static final int ERROR_STALE_CONFIG = -2;
    public static final int ERROR_UNKNOWN = -3;
    public static final int EVENT_DEVICE_AVAILABLE = 1;
    public static final int EVENT_DEVICE_UNAVAILABLE = 2;
    public static final int EVENT_FIRST_FRAME_CAPTURED = 4;
    public static final int EVENT_STREAM_CONFIGURATION_CHANGED = 3;
    public static final int SUCCESS = 0;
    private static final String TAG = TvInputHal.class.getSimpleName();
    private final Callback mCallback;
    private final Object mLock = new Object();
    private long mPtr = 0;
    private final SparseIntArray mStreamConfigGenerations = new SparseIntArray();
    private final SparseArray<TvStreamConfig[]> mStreamConfigs = new SparseArray<>();
    private final Queue<Message> mPendingMessageQueue = new LinkedList();
    private final Handler mHandler = new Handler(this);

    public interface Callback {
        void onDeviceAvailable(TvInputHardwareInfo tvInputHardwareInfo, TvStreamConfig[] tvStreamConfigArr);

        void onDeviceUnavailable(int i);

        void onFirstFrameCaptured(int i, int i2);

        void onStreamConfigurationChanged(int i, TvStreamConfig[] tvStreamConfigArr);
    }

    private static native int nativeAddOrUpdateStream(long j, int i, int i2, Surface surface);

    private static native void nativeClose(long j);

    private static native TvStreamConfig[] nativeGetStreamConfigs(long j, int i, int i2);

    private native long nativeOpen(MessageQueue messageQueue);

    private static native int nativeRemoveStream(long j, int i, int i2);

    public TvInputHal(Callback callback) {
        this.mCallback = callback;
    }

    public void init() {
        synchronized (this.mLock) {
            this.mPtr = nativeOpen(this.mHandler.getLooper().getQueue());
        }
    }

    public int addOrUpdateStream(int i, Surface surface, TvStreamConfig tvStreamConfig) {
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                return -1;
            }
            if (this.mStreamConfigGenerations.get(i, 0) != tvStreamConfig.getGeneration()) {
                return -2;
            }
            return nativeAddOrUpdateStream(this.mPtr, i, tvStreamConfig.getStreamId(), surface) == 0 ? 0 : -3;
        }
    }

    public int removeStream(int i, TvStreamConfig tvStreamConfig) {
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                return -1;
            }
            if (this.mStreamConfigGenerations.get(i, 0) != tvStreamConfig.getGeneration()) {
                return -2;
            }
            return nativeRemoveStream(this.mPtr, i, tvStreamConfig.getStreamId()) == 0 ? 0 : -3;
        }
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mPtr != 0) {
                nativeClose(this.mPtr);
            }
        }
    }

    private void retrieveStreamConfigsLocked(int i) {
        int i2 = this.mStreamConfigGenerations.get(i, 0) + 1;
        this.mStreamConfigs.put(i, nativeGetStreamConfigs(this.mPtr, i, i2));
        this.mStreamConfigGenerations.put(i, i2);
    }

    private void deviceAvailableFromNative(TvInputHardwareInfo tvInputHardwareInfo) {
        this.mHandler.obtainMessage(1, tvInputHardwareInfo).sendToTarget();
    }

    private void deviceUnavailableFromNative(int i) {
        this.mHandler.obtainMessage(2, i, 0).sendToTarget();
    }

    private void streamConfigsChangedFromNative(int i) {
        this.mHandler.obtainMessage(3, i, 0).sendToTarget();
    }

    private void firstFrameCapturedFromNative(int i, int i2) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, i, i2));
    }

    @Override
    public boolean handleMessage(Message message) {
        TvStreamConfig[] tvStreamConfigArr;
        TvStreamConfig[] tvStreamConfigArr2;
        switch (message.what) {
            case 1:
                TvInputHardwareInfo tvInputHardwareInfo = (TvInputHardwareInfo) message.obj;
                synchronized (this.mLock) {
                    retrieveStreamConfigsLocked(tvInputHardwareInfo.getDeviceId());
                    tvStreamConfigArr = this.mStreamConfigs.get(tvInputHardwareInfo.getDeviceId());
                    break;
                }
                this.mCallback.onDeviceAvailable(tvInputHardwareInfo, tvStreamConfigArr);
                return true;
            case 2:
                this.mCallback.onDeviceUnavailable(message.arg1);
                return true;
            case 3:
                int i = message.arg1;
                synchronized (this.mLock) {
                    retrieveStreamConfigsLocked(i);
                    tvStreamConfigArr2 = this.mStreamConfigs.get(i);
                    break;
                }
                this.mCallback.onStreamConfigurationChanged(i, tvStreamConfigArr2);
                return true;
            case 4:
                this.mCallback.onFirstFrameCaptured(message.arg1, message.arg2);
                return true;
            default:
                Slog.e(TAG, "Unknown event: " + message);
                return false;
        }
    }
}
