package com.android.server.location;

import android.location.IGpsStatusListener;
import android.os.Handler;
import android.os.RemoteException;
import com.android.server.location.RemoteListenerHelper;

abstract class GpsStatusListenerHelper extends RemoteListenerHelper<IGpsStatusListener> {
    public GpsStatusListenerHelper(Handler handler) {
        super(handler, "GpsStatusListenerHelper");
        Operation nullOperation = new Operation() {
            @Override
            public void execute(IGpsStatusListener iGpsStatusListener) throws RemoteException {
            }
        };
        setSupported(GpsLocationProvider.isSupported(), nullOperation);
    }

    @Override
    protected boolean registerWithService() {
        return true;
    }

    @Override
    protected void unregisterFromService() {
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGpsStatusListener> getHandlerOperation(int result) {
        return null;
    }

    @Override
    protected void handleGpsEnabledChanged(boolean enabled) {
        Operation operation;
        if (enabled) {
            operation = new Operation() {
                @Override
                public void execute(IGpsStatusListener listener) throws RemoteException {
                    listener.onGpsStarted();
                }
            };
        } else {
            operation = new Operation() {
                @Override
                public void execute(IGpsStatusListener listener) throws RemoteException {
                    listener.onGpsStopped();
                }
            };
        }
        foreach(operation);
    }

    public void onFirstFix(final int timeToFirstFix) {
        Operation operation = new Operation() {
            {
                super();
            }

            @Override
            public void execute(IGpsStatusListener listener) throws RemoteException {
                listener.onFirstFix(timeToFirstFix);
            }
        };
        foreach(operation);
    }

    public void onSvStatusChanged(final int svCount, final int[] prns, final float[] snrs, final float[] elevations, final float[] azimuths, final int ephemerisMask, final int almanacMask, final int usedInFixMask) {
        Operation operation = new Operation() {
            {
                super();
            }

            @Override
            public void execute(IGpsStatusListener listener) throws RemoteException {
                listener.onSvStatusChanged(svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
            }
        };
        foreach(operation);
    }

    public void onNmeaReceived(final long timestamp, final String nmea) {
        Operation operation = new Operation() {
            {
                super();
            }

            @Override
            public void execute(IGpsStatusListener listener) throws RemoteException {
                listener.onNmeaReceived(timestamp, nmea);
            }
        };
        foreach(operation);
    }

    private abstract class Operation implements RemoteListenerHelper.ListenerOperation<IGpsStatusListener> {
        private Operation() {
        }
    }
}
