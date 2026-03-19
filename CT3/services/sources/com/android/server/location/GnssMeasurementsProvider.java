package com.android.server.location;

import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.os.Handler;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.location.RemoteListenerHelper;

public abstract class GnssMeasurementsProvider extends RemoteListenerHelper<IGnssMeasurementsListener> {
    private static final String TAG = "GnssMeasurementsProvider";

    @Override
    public boolean addListener(IInterface listener) {
        return super.addListener(listener);
    }

    @Override
    public void removeListener(IInterface listener) {
        super.removeListener(listener);
    }

    protected GnssMeasurementsProvider(Handler handler) {
        super(handler, TAG);
    }

    public void onMeasurementsAvailable(final GnssMeasurementsEvent event) {
        RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener> operation = new RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener>() {
            @Override
            public void execute(IGnssMeasurementsListener listener) throws RemoteException {
                listener.onGnssMeasurementsReceived(event);
            }
        };
        foreach(operation);
    }

    public void onCapabilitiesUpdated(boolean isGnssMeasurementsSupported) {
        setSupported(isGnssMeasurementsSupported);
        updateResult();
    }

    public void onGpsEnabledChanged() {
        if (!tryUpdateRegistrationWithService()) {
            return;
        }
        updateResult();
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener> getHandlerOperation(int result) {
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

    private static class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener> {
        private final int mStatus;

        public StatusChangedOperation(int status) {
            this.mStatus = status;
        }

        @Override
        public void execute(IGnssMeasurementsListener listener) throws RemoteException {
            listener.onStatusChanged(this.mStatus);
        }
    }
}
