package com.android.server.broadcastradio.hal2;

import android.graphics.Bitmap;
import android.hardware.broadcastradio.V2_0.ConfigFlag;
import android.hardware.broadcastradio.V2_0.ITunerSession;
import android.hardware.radio.ITuner;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.RemoteException;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Slog;
import com.android.server.broadcastradio.hal2.TunerCallback;
import com.android.server.broadcastradio.hal2.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class TunerSession extends ITuner.Stub {
    private static final String TAG = "BcRadio2Srv.session";
    private static final String kAudioDeviceName = "Radio tuner source";
    private final TunerCallback mCallback;
    private final ITunerSession mHwSession;
    private final RadioModule mModule;
    private final Object mLock = new Object();
    private boolean mIsClosed = false;
    private boolean mIsMuted = false;
    private RadioManager.BandConfig mDummyConfig = null;

    TunerSession(RadioModule radioModule, ITunerSession iTunerSession, TunerCallback tunerCallback) {
        this.mModule = (RadioModule) Objects.requireNonNull(radioModule);
        this.mHwSession = (ITunerSession) Objects.requireNonNull(iTunerSession);
        this.mCallback = (TunerCallback) Objects.requireNonNull(tunerCallback);
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
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

    public void setConfiguration(final RadioManager.BandConfig bandConfig) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            this.mDummyConfig = (RadioManager.BandConfig) Objects.requireNonNull(bandConfig);
            Slog.i(TAG, "Ignoring setConfiguration - not applicable for broadcastradio HAL 2.x");
            TunerCallback.dispatch(new TunerCallback.RunnableThrowingRemoteException() {
                @Override
                public final void run() {
                    this.f$0.mCallback.mClientCb.onConfigurationChanged(bandConfig);
                }
            });
        }
    }

    public RadioManager.BandConfig getConfiguration() {
        RadioManager.BandConfig bandConfig;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            bandConfig = this.mDummyConfig;
        }
        return bandConfig;
    }

    public void setMuted(boolean z) {
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
        synchronized (this.mLock) {
            checkNotClosedLocked();
            z = this.mIsMuted;
        }
        return z;
    }

    public void step(boolean z, boolean z2) throws RemoteException {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            Convert.throwOnError("step", this.mHwSession.step(!z));
        }
    }

    public void scan(boolean z, boolean z2) throws RemoteException {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            Convert.throwOnError("step", this.mHwSession.scan(!z, z2));
        }
    }

    public void tune(ProgramSelector programSelector) throws RemoteException {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            Convert.throwOnError("tune", this.mHwSession.tune(Convert.programSelectorToHal(programSelector)));
        }
    }

    public void cancel() {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            final ITunerSession iTunerSession = this.mHwSession;
            Objects.requireNonNull(iTunerSession);
            Utils.maybeRethrow(new Utils.VoidFuncThrowingRemoteException() {
                @Override
                public final void exec() throws RemoteException {
                    iTunerSession.cancel();
                }
            });
        }
    }

    public void cancelAnnouncement() {
        Slog.i(TAG, "Announcements control doesn't involve cancelling at the HAL level in 2.x");
    }

    public Bitmap getImage(int i) {
        return this.mModule.getImage(i);
    }

    public boolean startBackgroundScan() {
        Slog.i(TAG, "Explicit background scan trigger is not supported with HAL 2.x");
        TunerCallback.dispatch(new TunerCallback.RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mCallback.mClientCb.onBackgroundScanComplete();
            }
        });
        return true;
    }

    public void startProgramListUpdates(ProgramList.Filter filter) throws RemoteException {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            Convert.throwOnError("startProgramListUpdates", this.mHwSession.startProgramListUpdates(Convert.programFilterToHal(filter)));
        }
    }

    public void stopProgramListUpdates() throws RemoteException {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            this.mHwSession.stopProgramListUpdates();
        }
    }

    public boolean isConfigFlagSupported(int i) {
        try {
            isConfigFlagSet(i);
            return true;
        } catch (IllegalStateException e) {
            return true;
        } catch (UnsupportedOperationException e2) {
            return false;
        }
    }

    public boolean isConfigFlagSet(int i) {
        boolean z;
        Slog.v(TAG, "isConfigFlagSet " + ConfigFlag.toString(i));
        synchronized (this.mLock) {
            checkNotClosedLocked();
            final MutableInt mutableInt = new MutableInt(1);
            final MutableBoolean mutableBoolean = new MutableBoolean(false);
            try {
                this.mHwSession.isConfigFlagSet(i, new ITunerSession.isConfigFlagSetCallback() {
                    @Override
                    public final void onValues(int i2, boolean z2) {
                        TunerSession.lambda$isConfigFlagSet$2(mutableInt, mutableBoolean, i2, z2);
                    }
                });
                Convert.throwOnError("isConfigFlagSet", mutableInt.value);
                z = mutableBoolean.value;
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to check flag " + ConfigFlag.toString(i), e);
            }
        }
        return z;
    }

    static void lambda$isConfigFlagSet$2(MutableInt mutableInt, MutableBoolean mutableBoolean, int i, boolean z) {
        mutableInt.value = i;
        mutableBoolean.value = z;
    }

    public void setConfigFlag(int i, boolean z) throws RemoteException {
        Slog.v(TAG, "setConfigFlag " + ConfigFlag.toString(i) + " = " + z);
        synchronized (this.mLock) {
            checkNotClosedLocked();
            Convert.throwOnError("setConfigFlag", this.mHwSession.setConfigFlag(i, z));
        }
    }

    public Map setParameters(final Map map) {
        Map<String, String> mapVendorInfoFromHal;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            mapVendorInfoFromHal = Convert.vendorInfoFromHal((List) Utils.maybeRethrow(new Utils.FuncThrowingRemoteException() {
                @Override
                public final Object exec() {
                    return this.f$0.mHwSession.setParameters(Convert.vendorInfoToHal(map));
                }
            }));
        }
        return mapVendorInfoFromHal;
    }

    public Map getParameters(final List<String> list) {
        Map<String, String> mapVendorInfoFromHal;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            mapVendorInfoFromHal = Convert.vendorInfoFromHal((List) Utils.maybeRethrow(new Utils.FuncThrowingRemoteException() {
                @Override
                public final Object exec() {
                    return this.f$0.mHwSession.getParameters(Convert.listToArrayList(list));
                }
            }));
        }
        return mapVendorInfoFromHal;
    }
}
