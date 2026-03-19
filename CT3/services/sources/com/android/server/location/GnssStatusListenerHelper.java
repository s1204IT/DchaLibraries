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
    protected boolean registerWithService() {
        return true;
    }

    @Override
    protected void unregisterFromService() {
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGnssStatusListener> getHandlerOperation(int result) {
        return null;
    }

    public void onStatusChanged(boolean isNavigating) {
        Operation operation;
        if (isNavigating) {
            operation = new Operation() {
                @Override
                public void execute(IGnssStatusListener listener) throws RemoteException {
                    listener.onGnssStarted();
                }
            };
        } else {
            operation = new Operation() {
                @Override
                public void execute(IGnssStatusListener listener) throws RemoteException {
                    listener.onGnssStopped();
                }
            };
        }
        foreach(operation);
    }

    public void onFirstFix(final int timeToFirstFix) {
        Operation operation = new Operation() {
            @Override
            public void execute(IGnssStatusListener listener) throws RemoteException {
                listener.onFirstFix(timeToFirstFix);
            }
        };
        foreach(operation);
    }

    public void onSvStatusChanged(final int svCount, final int[] prnWithFlags, final float[] cn0s, final float[] elevations, final float[] azimuths) {
        Operation operation = new Operation() {
            @Override
            public void execute(IGnssStatusListener listener) throws RemoteException {
                listener.onSvStatusChanged(svCount, prnWithFlags, cn0s, elevations, azimuths);
            }
        };
        foreach(operation);
    }

    public void onNmeaReceived(final long timestamp, final String nmea) {
        Operation operation = new Operation() {
            @Override
            public void execute(IGnssStatusListener listener) throws RemoteException {
                listener.onNmeaReceived(timestamp, nmea);
            }
        };
        foreach(operation);
    }
}
