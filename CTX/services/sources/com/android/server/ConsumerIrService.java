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
    private final boolean mHasNativeHal;
    private final PowerManager.WakeLock mWakeLock;

    private static native int[] halGetCarrierFrequencies();

    private static native boolean halOpen();

    private static native int halTransmit(int i, int[] iArr);

    ConsumerIrService(Context context) {
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, TAG);
        this.mWakeLock.setReferenceCounted(true);
        this.mHasNativeHal = halOpen();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.consumerir")) {
            if (!this.mHasNativeHal) {
                throw new RuntimeException("FEATURE_CONSUMER_IR present, but no IR HAL loaded!");
            }
        } else if (this.mHasNativeHal) {
            throw new RuntimeException("IR HAL present, but FEATURE_CONSUMER_IR is not set!");
        }
    }

    public boolean hasIrEmitter() {
        return this.mHasNativeHal;
    }

    private void throwIfNoIrEmitter() {
        if (!this.mHasNativeHal) {
            throw new UnsupportedOperationException("IR emitter not available");
        }
    }

    public void transmit(String str, int i, int[] iArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR") != 0) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        long j = 0;
        for (int i2 : iArr) {
            if (i2 <= 0) {
                throw new IllegalArgumentException("Non-positive IR slice");
            }
            j += (long) i2;
        }
        if (j > 2000000) {
            throw new IllegalArgumentException("IR pattern too long");
        }
        throwIfNoIrEmitter();
        synchronized (this.mHalLock) {
            int iHalTransmit = halTransmit(i, iArr);
            if (iHalTransmit < 0) {
                Slog.e(TAG, "Error transmitting: " + iHalTransmit);
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
            iArrHalGetCarrierFrequencies = halGetCarrierFrequencies();
        }
        return iArrHalGetCarrierFrequencies;
    }
}
