package android.location;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GnssMeasurementCallbackTransport extends LocalListenerHelper<GnssMeasurementsEvent.Callback> {
    private final IGnssMeasurementsListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GnssMeasurementCallbackTransport(Context context, ILocationManager locationManager) {
        super(context, "GnssMeasurementListenerTransport");
        this.mListenerTransport = new ListenerTransport(this, null);
        this.mLocationManager = locationManager;
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
        ListenerTransport(GnssMeasurementCallbackTransport this$0, ListenerTransport listenerTransport) {
            this();
        }

        private ListenerTransport() {
        }

        @Override
        public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
            LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback> operation = new LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback>() {
                @Override
                public void execute(GnssMeasurementsEvent.Callback callback) throws RemoteException {
                    callback.onGnssMeasurementsReceived(event);
                }
            };
            GnssMeasurementCallbackTransport.this.foreach(operation);
        }

        @Override
        public void onStatusChanged(final int status) {
            LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback> operation = new LocalListenerHelper.ListenerOperation<GnssMeasurementsEvent.Callback>() {
                @Override
                public void execute(GnssMeasurementsEvent.Callback callback) throws RemoteException {
                    callback.onStatusChanged(status);
                }
            };
            GnssMeasurementCallbackTransport.this.foreach(operation);
        }
    }
}
