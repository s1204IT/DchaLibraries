package com.android.server.broadcastradio.hal2;

import android.hardware.broadcastradio.V2_0.ITunerCallback;
import android.hardware.broadcastradio.V2_0.ProgramInfo;
import android.hardware.broadcastradio.V2_0.ProgramListChunk;
import android.hardware.broadcastradio.V2_0.ProgramSelector;
import android.hardware.broadcastradio.V2_0.VendorKeyValue;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Objects;

class TunerCallback extends ITunerCallback.Stub {
    private static final String TAG = "BcRadio2Srv.cb";
    final android.hardware.radio.ITunerCallback mClientCb;

    interface RunnableThrowingRemoteException {
        void run() throws RemoteException;
    }

    TunerCallback(android.hardware.radio.ITunerCallback iTunerCallback) {
        this.mClientCb = (android.hardware.radio.ITunerCallback) Objects.requireNonNull(iTunerCallback);
    }

    static void dispatch(RunnableThrowingRemoteException runnableThrowingRemoteException) {
        try {
            runnableThrowingRemoteException.run();
        } catch (RemoteException e) {
            Slog.e(TAG, "callback call failed", e);
        }
    }

    @Override
    public void onTuneFailed(final int i, final ProgramSelector programSelector) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCb.onTuneFailed(i, Convert.programSelectorFromHal(programSelector));
            }
        });
    }

    @Override
    public void onCurrentProgramInfoChanged(final ProgramInfo programInfo) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCb.onCurrentProgramInfoChanged(Convert.programInfoFromHal(programInfo));
            }
        });
    }

    @Override
    public void onProgramListUpdated(final ProgramListChunk programListChunk) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCb.onProgramListUpdated(Convert.programListChunkFromHal(programListChunk));
            }
        });
    }

    @Override
    public void onAntennaStateChange(final boolean z) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCb.onAntennaState(z);
            }
        });
    }

    @Override
    public void onParametersUpdated(final ArrayList<VendorKeyValue> arrayList) {
        dispatch(new RunnableThrowingRemoteException() {
            @Override
            public final void run() {
                this.f$0.mClientCb.onParametersUpdated(Convert.vendorInfoFromHal(arrayList));
            }
        });
    }
}
