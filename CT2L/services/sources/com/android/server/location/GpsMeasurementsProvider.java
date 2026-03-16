package com.android.server.location;

import android.location.GpsMeasurementsEvent;
import android.location.IGpsMeasurementsListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.location.RemoteListenerHelper;

public abstract class GpsMeasurementsProvider extends RemoteListenerHelper<IGpsMeasurementsListener> {
    private static final String TAG = "GpsMeasurementsProvider";

    @Override
    public void onGpsEnabledChanged(boolean z) {
        super.onGpsEnabledChanged(z);
    }

    public GpsMeasurementsProvider(Handler handler) {
        super(handler, TAG);
    }

    public void onMeasurementsAvailable(final GpsMeasurementsEvent event) {
        RemoteListenerHelper.ListenerOperation<IGpsMeasurementsListener> operation = new RemoteListenerHelper.ListenerOperation<IGpsMeasurementsListener>() {
            @Override
            public void execute(IGpsMeasurementsListener listener) throws RemoteException {
                listener.onGpsMeasurementsReceived(event);
            }
        };
        foreach(operation);
    }

    public void onCapabilitiesUpdated(boolean isGpsMeasurementsSupported) {
        int status = isGpsMeasurementsSupported ? 1 : 0;
        setSupported(isGpsMeasurementsSupported, new StatusChangedOperation(status));
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGpsMeasurementsListener> getHandlerOperation(int result) {
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
            default:
                Log.v(TAG, "Unhandled addListener result: " + result);
                return null;
        }
        return new StatusChangedOperation(status);
    }

    @Override
    protected void handleGpsEnabledChanged(boolean enabled) {
        int status = enabled ? 1 : 2;
        foreach(new StatusChangedOperation(status));
    }

    private class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGpsMeasurementsListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        @Override
        public void execute(IGpsMeasurementsListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }
}
