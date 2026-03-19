package android.location;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GnssMeasurementCallbackTransport extends LocalListenerHelper<GnssMeasurementsEvent.Callback> {
    private final IGnssMeasurementsListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GnssMeasurementCallbackTransport(Context context, ILocationManager iLocationManager) {
        super(context, "GnssMeasurementListenerTransport");
        this.mListenerTransport = new ListenerTransport();
        this.mLocationManager = iLocationManager;
    }

    @Override
    protected boolean registerWithServer() throws RemoteException {
        return this.mLocationManager.addGnssMeasurementsListener(this.mListenerTransport, getContext().getPackageName());
    }

    @Override
    protected void unregisterFromServer() throws RemoteException {
        this.mLocationManager.removeGnssMeasurementsListener(this.mListenerTransport);
    }

    private class ListenerTransport extends IGnssMeasurementsListener.Stub {
        private ListenerTransport() {
        }

        @Override
        public void onGnssMeasurementsReceived(final GnssMeasurementsEvent gnssMeasurementsEvent) {
            GnssMeasurementCallbackTransport.this.foreach(new LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback>() {
                @Override
                public void execute(GnssMeasurementsEvent.Callback callback) throws RemoteException {
                    callback.onGnssMeasurementsReceived(gnssMeasurementsEvent);
                }
            });
        }

        @Override
        public void onStatusChanged(final int i) {
            GnssMeasurementCallbackTransport.this.foreach(new LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback>() {
                @Override
                public void execute(GnssMeasurementsEvent.Callback callback) throws RemoteException {
                    callback.onStatusChanged(i);
                }
            });
        }
    }
}
