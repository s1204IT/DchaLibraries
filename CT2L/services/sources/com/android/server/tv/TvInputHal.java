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

    public int addOrUpdateStream(int deviceId, Surface surface, TvStreamConfig streamConfig) {
        int i = 0;
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                i = -1;
            } else {
                int generation = this.mStreamConfigGenerations.get(deviceId, 0);
                if (generation != streamConfig.getGeneration()) {
                    i = -2;
                } else if (nativeAddOrUpdateStream(this.mPtr, deviceId, streamConfig.getStreamId(), surface) != 0) {
                    i = -3;
                }
            }
        }
        return i;
    }

    public int removeStream(int deviceId, TvStreamConfig streamConfig) {
        int i = 0;
        synchronized (this.mLock) {
            if (this.mPtr == 0) {
                i = -1;
            } else {
                int generation = this.mStreamConfigGenerations.get(deviceId, 0);
                if (generation != streamConfig.getGeneration()) {
                    i = -2;
                } else if (nativeRemoveStream(this.mPtr, deviceId, streamConfig.getStreamId()) != 0) {
                    i = -3;
                }
            }
        }
        return i;
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mPtr != 0) {
                nativeClose(this.mPtr);
            }
        }
    }

    private void retrieveStreamConfigsLocked(int deviceId) {
        int generation = this.mStreamConfigGenerations.get(deviceId, 0) + 1;
        this.mStreamConfigs.put(deviceId, nativeGetStreamConfigs(this.mPtr, deviceId, generation));
        this.mStreamConfigGenerations.put(deviceId, generation);
    }

    private void deviceAvailableFromNative(TvInputHardwareInfo info) {
        this.mHandler.obtainMessage(1, info).sendToTarget();
    }

    private void deviceUnavailableFromNative(int deviceId) {
        this.mHandler.obtainMessage(2, deviceId, 0).sendToTarget();
    }

    private void streamConfigsChangedFromNative(int deviceId) {
        this.mHandler.obtainMessage(3, deviceId, 0).sendToTarget();
    }

    private void firstFrameCapturedFromNative(int deviceId, int streamId) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, deviceId, streamId));
    }

    @Override
    public boolean handleMessage(Message msg) {
        TvStreamConfig[] configs;
        TvStreamConfig[] configs2;
        switch (msg.what) {
            case 1:
                TvInputHardwareInfo info = (TvInputHardwareInfo) msg.obj;
                synchronized (this.mLock) {
                    retrieveStreamConfigsLocked(info.getDeviceId());
                    configs2 = this.mStreamConfigs.get(info.getDeviceId());
                    break;
                }
                this.mCallback.onDeviceAvailable(info, configs2);
                return true;
            case 2:
                int deviceId = msg.arg1;
                this.mCallback.onDeviceUnavailable(deviceId);
                return true;
            case 3:
                int deviceId2 = msg.arg1;
                synchronized (this.mLock) {
                    retrieveStreamConfigsLocked(deviceId2);
                    configs = this.mStreamConfigs.get(deviceId2);
                    break;
                }
                this.mCallback.onStreamConfigurationChanged(deviceId2, configs);
                return true;
            case 4:
                int deviceId3 = msg.arg1;
                int streamId = msg.arg2;
                this.mCallback.onFirstFrameCaptured(deviceId3, streamId);
                return true;
            default:
                Slog.e(TAG, "Unknown event: " + msg);
                return DEBUG;
        }
    }
}
