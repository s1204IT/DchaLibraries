package com.android.server.location;

import android.location.IGnssStatusListener;
import android.os.Handler;
import android.os.RemoteException;
import com.android.server.location.RemoteListenerHelper;

abstract class GnssStatusListenerHelper extends RemoteListenerHelper<IGnssStatusListener> {

    private interface Operation extends RemoteListenerHelper.ListenerOperation<IGnssStatusListener> {
    }

    protected GnssStatusListenerHelper(Handler handler) {
        super(handler, "GnssStatusListenerHelper");
        setSupported(GnssLocationProvider.isSupported());
    }

    @Override
    protected int registerWithService() {
        return 0;
    }

    @Override
    protected void unregisterFromService() {
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGnssStatusListener> getHandlerOperation(int i) {
        return null;
    }

    public void onStatusChanged(boolean z) {
        Operation operation;
        if (z) {
            operation = new Operation() {
                @Override
                public void execute(IGnssStatusListener iGnssStatusListener) throws RemoteException {
                    iGnssStatusListener.onGnssStarted();
                }
            };
        } else {
            operation = new Operation() {
                @Override
                public void execute(IGnssStatusListener iGnssStatusListener) throws RemoteException {
                    iGnssStatusListener.onGnssStopped();
                }
            };
        }
        foreach(operation);
    }

    public void onFirstFix(final int i) {
        foreach(new Operation() {
            @Override
            public void execute(IGnssStatusListener iGnssStatusListener) throws RemoteException {
                iGnssStatusListener.onFirstFix(i);
            }
        });
    }

    public void onSvStatusChanged(final int i, final int[] iArr, final float[] fArr, final float[] fArr2, final float[] fArr3, final float[] fArr4) {
        foreach(new Operation() {
            @Override
            public void execute(IGnssStatusListener iGnssStatusListener) throws RemoteException {
                iGnssStatusListener.onSvStatusChanged(i, iArr, fArr, fArr2, fArr3, fArr4);
            }
        });
    }

    public void onNmeaReceived(final long j, final String str) {
        foreach(new Operation() {
            @Override
            public void execute(IGnssStatusListener iGnssStatusListener) throws RemoteException {
                iGnssStatusListener.onNmeaReceived(j, str);
            }
        });
    }
}
