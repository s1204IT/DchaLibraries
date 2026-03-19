package com.android.server;

import android.content.Context;
import android.hardware.IConsumerIrService;
import android.os.PowerManager;
import android.util.Slog;

public class ConsumerIrService extends IConsumerIrService.Stub {
    private static final int MAX_XMIT_TIME = 2000000;
    private static final String TAG = "ConsumerIrService";
    private final Context mContext;
    private final Object mHalLock = new Object();
    private final long mNativeHal;
    private final PowerManager.WakeLock mWakeLock;

    private static native int[] halGetCarrierFrequencies(long j);

    private static native long halOpen();

    private static native int halTransmit(long j, int i, int[] iArr);

    ConsumerIrService(Context context) {
        this.mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, TAG);
        this.mWakeLock.setReferenceCounted(true);
        this.mNativeHal = halOpen();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.consumerir")) {
            if (this.mNativeHal != 0) {
            } else {
                throw new RuntimeException("FEATURE_CONSUMER_IR present, but no IR HAL loaded!");
            }
        } else if (this.mNativeHal == 0) {
        } else {
            throw new RuntimeException("IR HAL present, but FEATURE_CONSUMER_IR is not set!");
        }
    }

    public boolean hasIrEmitter() {
        return this.mNativeHal != 0;
    }

    private void throwIfNoIrEmitter() {
        if (this.mNativeHal != 0) {
        } else {
            throw new UnsupportedOperationException("IR emitter not available");
        }
    }

    public void transmit(String packageName, int carrierFrequency, int[] pattern) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR") != 0) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        long totalXmitTime = 0;
        for (int slice : pattern) {
            if (slice <= 0) {
                throw new IllegalArgumentException("Non-positive IR slice");
            }
            totalXmitTime += (long) slice;
        }
        if (totalXmitTime > 2000000) {
            throw new IllegalArgumentException("IR pattern too long");
        }
        throwIfNoIrEmitter();
        synchronized (this.mHalLock) {
            int err = halTransmit(this.mNativeHal, carrierFrequency, pattern);
            if (err < 0) {
                Slog.e(TAG, "Error transmitting: " + err);
            }
        }
    }

    public int[] getCarrierFrequencies() {
        int[] iArrHalGetCarrierFrequencies;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR") != 0) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        throwIfNoIrEmitter();
        synchronized (this.mHalLock) {
            iArrHalGetCarrierFrequencies = halGetCarrierFrequencies(this.mNativeHal);
        }
        return iArrHalGetCarrierFrequencies;
    }
}
