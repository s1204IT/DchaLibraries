package com.android.server.broadcastradio.hal1;

import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import java.util.List;
import java.util.Objects;

public class BroadcastRadioService {
    private final long mNativeContext = nativeInit();
    private final Object mLock = new Object();

    private native void nativeFinalize(long j);

    private native long nativeInit();

    private native List<RadioManager.ModuleProperties> nativeLoadModules(long j);

    private native Tuner nativeOpenTuner(long j, int i, RadioManager.BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback);

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public List<RadioManager.ModuleProperties> loadModules() {
        List<RadioManager.ModuleProperties> list;
        synchronized (this.mLock) {
            list = (List) Objects.requireNonNull(nativeLoadModules(this.mNativeContext));
        }
        return list;
    }

    public ITuner openTuner(int i, RadioManager.BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback) {
        Tuner tunerNativeOpenTuner;
        synchronized (this.mLock) {
            tunerNativeOpenTuner = nativeOpenTuner(this.mNativeContext, i, bandConfig, z, iTunerCallback);
        }
        return tunerNativeOpenTuner;
    }
}
