package com.android.server.location;

import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.os.Handler;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.location.RemoteListenerHelper;

public abstract class GnssNavigationMessageProvider extends RemoteListenerHelper<IGnssNavigationMessageListener> {
    private static final String TAG = "GnssNavigationMessageProvider";

    @Override
    public boolean addListener(IInterface listener) {
        return super.addListener(listener);
    }

    @Override
    public void removeListener(IInterface listener) {
        super.removeListener(listener);
    }

    protected GnssNavigationMessageProvider(Handler handler) {
        super(handler, TAG);
    }

    public void onNavigationMessageAvailable(final GnssNavigationMessage event) {
        RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener> operation = new RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener>() {
            @Override
            public void execute(IGnssNavigationMessageListener listener) throws RemoteException {
                listener.onGnssNavigationMessageReceived(event);
            }
        };
        foreach(operation);
    }

    public void onCapabilitiesUpdated(boolean isGnssNavigationMessageSupported) {
        setSupported(isGnssNavigationMessageSupported);
        updateResult();
    }

    public void onGpsEnabledChanged() {
        if (!tryUpdateRegistrationWithService()) {
            return;
        }
        updateResult();
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener> getHandlerOperation(int result) {
        int status;
        switch (result) {
            case 0:
                status = 1;
                break;
            case 1:
            case 2:
            case 4:
                status = 0;
                break;
            case 3:
                status = 2;
                break;
            case 5:
                return null;
            default:
                Log.v(TAG, "Unhandled addListener result: " + result);
                return null;
        }
        return new StatusChangedOperation(status);
    }

    private static class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        @Override
        public void execute(IGnssNavigationMessageListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }
}
