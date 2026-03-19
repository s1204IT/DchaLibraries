package com.android.server.broadcastradio.hal1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.List;
import java.util.Map;

class Tuner extends ITuner.Stub {
    private static final String TAG = "BroadcastRadioService.Tuner";
    private final ITunerCallback mClientCallback;
    private final long mNativeContext;
    private int mRegion;
    private final TunerCallback mTunerCallback;
    private final boolean mWithAudio;
    private final Object mLock = new Object();
    private boolean mIsClosed = false;
    private boolean mIsMuted = false;
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public final void binderDied() {
            this.f$0.close();
        }
    };

    private native void nativeCancel(long j);

    private native void nativeCancelAnnouncement(long j);

    private native void nativeClose(long j);

    private native void nativeFinalize(long j);

    private native RadioManager.BandConfig nativeGetConfiguration(long j, int i);

    private native byte[] nativeGetImage(long j, int i);

    private native List<RadioManager.ProgramInfo> nativeGetProgramList(long j, Map<String, String> map);

    private native long nativeInit(int i, boolean z, int i2);

    private native boolean nativeIsAnalogForced(long j);

    private native void nativeScan(long j, boolean z, boolean z2);

    private native void nativeSetAnalogForced(long j, boolean z);

    private native void nativeSetConfiguration(long j, RadioManager.BandConfig bandConfig);

    private native boolean nativeStartBackgroundScan(long j);

    private native void nativeStep(long j, boolean z, boolean z2);

    private native void nativeTune(long j, ProgramSelector programSelector);

    Tuner(ITunerCallback iTunerCallback, int i, int i2, boolean z, int i3) {
        this.mClientCallback = iTunerCallback;
        this.mTunerCallback = new TunerCallback(this, iTunerCallback, i);
        this.mRegion = i2;
        this.mWithAudio = z;
        this.mNativeContext = nativeInit(i, z, i3);
        try {
            this.mClientCallback.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (RemoteException e) {
            close();
        }
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super/*java.lang.Object*/.finalize();
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mTunerCallback.detach();
            this.mClientCallback.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            nativeClose(this.mNativeContext);
        }
    }

    public boolean isClosed() {
        return this.mIsClosed;
    }

    private void checkNotClosedLocked() {
        if (this.mIsClosed) {
            throw new IllegalStateException("Tuner is closed, no further operations are allowed");
        }
    }

    private boolean checkConfiguredLocked() {
        if (this.mTunerCallback.isInitialConfigurationDone()) {
            return true;
        }
        Slog.w(TAG, "Initial configuration is still pending, skipping the operation");
        return false;
    }

    public void setConfiguration(RadioManager.BandConfig bandConfig) {
        if (bandConfig == null) {
            throw new IllegalArgumentException("The argument must not be a null pointer");
        }
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeSetConfiguration(this.mNativeContext, bandConfig);
            this.mRegion = bandConfig.getRegion();
        }
    }

    public RadioManager.BandConfig getConfiguration() {
        RadioManager.BandConfig bandConfigNativeGetConfiguration;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            bandConfigNativeGetConfiguration = nativeGetConfiguration(this.mNativeContext, this.mRegion);
        }
        return bandConfigNativeGetConfiguration;
    }

    public void setMuted(boolean z) {
        if (!this.mWithAudio) {
            throw new IllegalStateException("Can't operate on mute - no audio requested");
        }
        synchronized (this.mLock) {
            checkNotClosedLocked();
            if (this.mIsMuted == z) {
                return;
            }
            this.mIsMuted = z;
            Slog.w(TAG, "Mute via RadioService is not implemented - please handle it via app");
        }
    }

    public boolean isMuted() {
        boolean z;
        if (!this.mWithAudio) {
            Slog.w(TAG, "Tuner did not request audio, pretending it was muted");
            return true;
        }
        synchronized (this.mLock) {
            checkNotClosedLocked();
            z = this.mIsMuted;
        }
        return z;
    }

    public void step(boolean z, boolean z2) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            if (checkConfiguredLocked()) {
                nativeStep(this.mNativeContext, z, z2);
            }
        }
    }

    public void scan(boolean z, boolean z2) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            if (checkConfiguredLocked()) {
                nativeScan(this.mNativeContext, z, z2);
            }
        }
    }

    public void tune(ProgramSelector programSelector) {
        if (programSelector == null) {
            throw new IllegalArgumentException("The argument must not be a null pointer");
        }
        Slog.i(TAG, "Tuning to " + programSelector);
        synchronized (this.mLock) {
            checkNotClosedLocked();
            if (checkConfiguredLocked()) {
                nativeTune(this.mNativeContext, programSelector);
            }
        }
    }

    public void cancel() {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeCancel(this.mNativeContext);
        }
    }

    public void cancelAnnouncement() {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeCancelAnnouncement(this.mNativeContext);
        }
    }

    public Bitmap getImage(int i) {
        byte[] bArrNativeGetImage;
        if (i == 0) {
            throw new IllegalArgumentException("Image ID is missing");
        }
        synchronized (this.mLock) {
            bArrNativeGetImage = nativeGetImage(this.mNativeContext, i);
        }
        if (bArrNativeGetImage == null || bArrNativeGetImage.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bArrNativeGetImage, 0, bArrNativeGetImage.length);
    }

    public boolean startBackgroundScan() {
        boolean zNativeStartBackgroundScan;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            zNativeStartBackgroundScan = nativeStartBackgroundScan(this.mNativeContext);
        }
        return zNativeStartBackgroundScan;
    }

    List<RadioManager.ProgramInfo> getProgramList(Map map) {
        List<RadioManager.ProgramInfo> listNativeGetProgramList;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            listNativeGetProgramList = nativeGetProgramList(this.mNativeContext, map);
            if (listNativeGetProgramList == null) {
                throw new IllegalStateException("Program list is not ready");
            }
        }
        return listNativeGetProgramList;
    }

    public void startProgramListUpdates(ProgramList.Filter filter) {
        this.mTunerCallback.startProgramListUpdates(filter);
    }

    public void stopProgramListUpdates() {
        this.mTunerCallback.stopProgramListUpdates();
    }

    public boolean isConfigFlagSupported(int i) {
        return i == 2;
    }

    public boolean isConfigFlagSet(int i) {
        boolean zNativeIsAnalogForced;
        if (i == 2) {
            synchronized (this.mLock) {
                checkNotClosedLocked();
                zNativeIsAnalogForced = nativeIsAnalogForced(this.mNativeContext);
            }
            return zNativeIsAnalogForced;
        }
        throw new UnsupportedOperationException("Not supported by HAL 1.x");
    }

    public void setConfigFlag(int i, boolean z) {
        if (i == 2) {
            synchronized (this.mLock) {
                checkNotClosedLocked();
                nativeSetAnalogForced(this.mNativeContext, z);
            }
            return;
        }
        throw new UnsupportedOperationException("Not supported by HAL 1.x");
    }

    public Map setParameters(Map map) {
        throw new UnsupportedOperationException("Not supported by HAL 1.x");
    }

    public Map getParameters(List<String> list) {
        throw new UnsupportedOperationException("Not supported by HAL 1.x");
    }
}
