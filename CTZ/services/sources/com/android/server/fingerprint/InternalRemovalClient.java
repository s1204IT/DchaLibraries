package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;

public abstract class InternalRemovalClient extends RemovalClient {
    public InternalRemovalClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, int i3, boolean z, String str) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i, i2, i3, z, str);
    }
}
