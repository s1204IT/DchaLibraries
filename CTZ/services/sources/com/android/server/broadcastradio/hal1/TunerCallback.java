package com.android.server.broadcastradio.hal1;

import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class TunerCallback implements ITunerCallback {
    private static final String TAG = "BroadcastRadioService.TunerCallback";
    private final ITunerCallback mClientCallback;
    private final long mNativeContext;
    private final Tuner mTuner;
    private final AtomicReference<ProgramList.Filter> mProgramListFilter = new AtomicReference<>();
    private boolean mInitialConfigurationDone = false;

    private interface RunnableThrowingRemoteException {
        void run() throws RemoteException;
    }

    private native void nativeDetach(long j);

    private native void nativeFinalize(long j);

    private native long nativeInit(Tuner tuner, int i);

    TunerCallback(Tuner tuner, ITunerCallback iTunerCallback, int i) {
        this.mTuner = tuner;
        this.mClientCallback = iTunerCallback;
        this.mNativeContext = nativeInit(tuner, i);
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public void detach() {
        nativeDetach(this.mNativeContext);
    }

    private void dispatch(RunnableThrowingRemoteException runnableThrowingRemoteException) {
        try {
            runnableThrowingRemoteException.run();
        } catch (RemoteException e) {
            Slog.e(TAG, "client died", e);
        }
    }

    private void handleHwFailure() {
        onError(0);
        this.mTuner.close();
    }

    void startProgramListUpdates(ProgramList.Filter filter) {
        if (filter == null) {
            filter = new ProgramList.Filter();
        }
        this.mProgramListFilter.set(filter);
        sendProgramListUpdate();
    }

    void stopProgramListUpdates() {
        this.mProgramListFilter.set(null);
    }

    boolean isInitialConfigurationDone() {
        return this.mInitialConfigurationDone;
    }

    public void onError(final int i) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onError(i);
            }
        });
    }

    public void onTuneFailed(int i, ProgramSelector programSelector) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public void onConfigurationChanged(final RadioManager.BandConfig bandConfig) {
        this.mInitialConfigurationDone = true;
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onConfigurationChanged(bandConfig);
            }
        });
    }

    public void onCurrentProgramInfoChanged(final RadioManager.ProgramInfo programInfo) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onCurrentProgramInfoChanged(programInfo);
            }
        });
    }

    public void onTrafficAnnouncement(final boolean z) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onTrafficAnnouncement(z);
            }
        });
    }

    public void onEmergencyAnnouncement(final boolean z) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onEmergencyAnnouncement(z);
            }
        });
    }

    public void onAntennaState(final boolean z) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onAntennaState(z);
            }
        });
    }

    public void onBackgroundScanAvailabilityChange(final boolean z) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onBackgroundScanAvailabilityChange(z);
            }
        });
    }

    public void onBackgroundScanComplete() {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onBackgroundScanComplete();
            }
        });
    }

    public void onProgramListChanged() {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onProgramListChanged();
            }
        });
        sendProgramListUpdate();
    }

    private void sendProgramListUpdate() {
        ProgramList.Filter filter = this.mProgramListFilter.get();
        if (filter == null) {
            return;
        }
        try {
            final ProgramList.Chunk chunk = new ProgramList.Chunk(true, true, (Set) this.mTuner.getProgramList(filter.getVendorFilter()).stream().collect(Collectors.toSet()), (Set) null);
            dispatch(new RunnableThrowingRemoteException() {
                @Override
                public final void run() {
                    this.f$0.mClientCallback.onProgramListUpdated(chunk);
                }
            });
        } catch (IllegalStateException e) {
            Slog.d(TAG, "Program list not ready yet");
        }
    }

    public void onProgramListUpdated(final ProgramList.Chunk chunk) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCallback.onProgramListUpdated(chunk);
            }
        });
    }

    public void onParametersUpdated(Map map) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public IBinder asBinder() {
        throw new RuntimeException("Not a binder");
    }
}
