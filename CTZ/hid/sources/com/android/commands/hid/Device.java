package com.android.commands.hid;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.os.SomeArgs;

public class Device {
    private static final int MSG_CLOSE_DEVICE = 3;
    private static final int MSG_OPEN_DEVICE = 1;
    private static final int MSG_SEND_REPORT = 2;
    private static final String TAG = "HidDevice";
    private final DeviceHandler mHandler;
    private final int mId;
    private long mTimeToSend;
    private final Object mCond = new Object();
    private final HandlerThread mThread = new HandlerThread("HidDeviceHandler");

    private static native void nativeCloseDevice(long j);

    private static native long nativeOpenDevice(String str, int i, int i2, int i3, byte[] bArr, DeviceCallback deviceCallback);

    private static native void nativeSendReport(long j, byte[] bArr);

    static {
        System.loadLibrary("hidcommand_jni");
    }

    public Device(int i, String str, int i2, int i3, byte[] bArr, byte[] bArr2) {
        this.mId = i;
        this.mThread.start();
        this.mHandler = new DeviceHandler(this.mThread.getLooper());
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.argi1 = i;
        someArgsObtain.argi2 = i2;
        someArgsObtain.argi3 = i3;
        if (str != null) {
            someArgsObtain.arg1 = str;
        } else {
            someArgsObtain.arg1 = i + ":" + i2 + ":" + i3;
        }
        someArgsObtain.arg2 = bArr;
        someArgsObtain.arg3 = bArr2;
        this.mHandler.obtainMessage(MSG_OPEN_DEVICE, someArgsObtain).sendToTarget();
        this.mTimeToSend = SystemClock.uptimeMillis();
    }

    public void sendReport(byte[] bArr) {
        this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(MSG_SEND_REPORT, bArr), this.mTimeToSend);
    }

    public void addDelay(int i) {
        this.mTimeToSend = Math.max(SystemClock.uptimeMillis(), this.mTimeToSend) + ((long) i);
    }

    public void close() {
        this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(MSG_CLOSE_DEVICE), Math.max(SystemClock.uptimeMillis(), this.mTimeToSend) + 1);
        try {
            synchronized (this.mCond) {
                this.mCond.wait();
            }
        } catch (InterruptedException e) {
        }
    }

    private class DeviceHandler extends Handler {
        private int mBarrierToken;
        private long mPtr;

        public DeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case Device.MSG_OPEN_DEVICE:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    this.mPtr = Device.nativeOpenDevice((String) someArgs.arg1, someArgs.argi1, someArgs.argi2, someArgs.argi3, (byte[]) someArgs.arg2, new DeviceCallback());
                    pauseEvents();
                    return;
                case Device.MSG_SEND_REPORT:
                    if (this.mPtr != 0) {
                        Device.nativeSendReport(this.mPtr, (byte[]) message.obj);
                        return;
                    } else {
                        Log.e(Device.TAG, "Tried to send report to closed device.");
                        return;
                    }
                case Device.MSG_CLOSE_DEVICE:
                    if (this.mPtr != 0) {
                        Device.nativeCloseDevice(this.mPtr);
                        getLooper().quitSafely();
                        this.mPtr = 0L;
                    } else {
                        Log.e(Device.TAG, "Tried to close already closed device.");
                    }
                    synchronized (Device.this.mCond) {
                        Device.this.mCond.notify();
                        break;
                    }
                    return;
                default:
                    throw new IllegalArgumentException("Unknown device message");
            }
        }

        public void pauseEvents() {
            getLooper();
            this.mBarrierToken = Looper.myQueue().postSyncBarrier();
        }

        public void resumeEvents() {
            getLooper();
            Looper.myQueue().removeSyncBarrier(this.mBarrierToken);
            this.mBarrierToken = 0;
        }
    }

    private class DeviceCallback {
        private DeviceCallback() {
        }

        public void onDeviceOpen() {
            Device.this.mHandler.resumeEvents();
        }

        public void onDeviceError() {
            Log.e(Device.TAG, "Device error occurred, closing /dev/uhid");
            Message messageObtainMessage = Device.this.mHandler.obtainMessage(Device.MSG_CLOSE_DEVICE);
            messageObtainMessage.setAsynchronous(true);
            messageObtainMessage.sendToTarget();
        }
    }
}
