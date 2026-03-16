package com.android.server.location;

import android.location.GpsNavigationMessageEvent;
import android.location.IGpsNavigationMessageListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.location.RemoteListenerHelper;

public abstract class GpsNavigationMessageProvider extends RemoteListenerHelper<IGpsNavigationMessageListener> {
    private static final String TAG = "GpsNavigationMessageProvider";

    @Override
    public void onGpsEnabledChanged(boolean z) {
        super.onGpsEnabledChanged(z);
    }

    public GpsNavigationMessageProvider(Handler handler) {
        super(handler, TAG);
    }

    public void onNavigationMessageAvailable(final GpsNavigationMessageEvent event) {
        RemoteListenerHelper.ListenerOperation<IGpsNavigationMessageListener> operation = new RemoteListenerHelper.ListenerOperation<IGpsNavigationMessageListener>() {
            @Override
            public void execute(IGpsNavigationMessageListener listener) throws RemoteException {
                listener.onGpsNavigationMessageReceived(event);
            }
        };
        foreach(operation);
    }

    public void onCapabilitiesUpdated(boolean isGpsNavigationMessageSupported) {
        int status = isGpsNavigationMessageSupported ? GpsNavigationMessageEvent.STATUS_READY : GpsNavigationMessageEvent.STATUS_NOT_SUPPORTED;
        setSupported(isGpsNavigationMessageSupported, new StatusChangedOperation(status));
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGpsNavigationMessageListener> getHandlerOperation(int result) {
        int status;
        switch (result) {
            case 0:
                status = GpsNavigationMessageEvent.STATUS_READY;
                break;
            case 1:
            case 2:
            case 4:
                status = GpsNavigationMessageEvent.STATUS_NOT_SUPPORTED;
                break;
            case 3:
                status = GpsNavigationMessageEvent.STATUS_GPS_LOCATION_DISABLED;
                break;
            default:
                Log.v(TAG, "Unhandled addListener result: " + result);
                return null;
        }
        return new StatusChangedOperation(status);
    }

    @Override
    protected void handleGpsEnabledChanged(boolean enabled) {
        int status = enabled ? GpsNavigationMessageEvent.STATUS_READY : GpsNavigationMessageEvent.STATUS_GPS_LOCATION_DISABLED;
        foreach(new StatusChangedOperation(status));
    }

    private class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGpsNavigationMessageListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        @Override
        public void execute(IGpsNavigationMessageListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }
}
