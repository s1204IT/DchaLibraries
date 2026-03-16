package android.location;

import android.content.Context;
import android.location.GpsMeasurementsEvent;
import android.location.IGpsMeasurementsListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GpsMeasurementListenerTransport extends LocalListenerHelper<GpsMeasurementsEvent.Listener> {
    private final IGpsMeasurementsListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GpsMeasurementListenerTransport(Context context, ILocationManager locationManager) {
        super(context, "GpsMeasurementListenerTransport");
        this.mListenerTransport = new ListenerTransport();
        this.mLocationManager = locationManager;
    }

    @Override
    protected boolean registerWithServer() throws RemoteException {
        return this.mLocationManager.addGpsMeasurementsListener(this.mListenerTransport, getContext().getPackageName());
    }

    @Override
    protected void unregisterFromServer() throws RemoteException {
        this.mLocationManager.removeGpsMeasurementsListener(this.mListenerTransport);
    }

    private class ListenerTransport extends IGpsMeasurementsListener.Stub {
        private ListenerTransport() {
        }

        @Override
        public void onGpsMeasurementsReceived(final GpsMeasurementsEvent event) {
            LocalListenerHelper.ListenerOperation<GpsMeasurementsEvent.Listener> operation = new LocalListenerHelper.ListenerOperation<GpsMeasurementsEvent.Listener>() {
                @Override
                public void execute(GpsMeasurementsEvent.Listener listener) throws RemoteException {
                    listener.onGpsMeasurementsReceived(event);
                }
            };
            GpsMeasurementListenerTransport.this.foreach(operation);
        }

        @Override
        public void onStatusChanged(final int status) {
            LocalListenerHelper.ListenerOperation<GpsMeasurementsEvent.Listener> operation = new LocalListenerHelper.ListenerOperation<GpsMeasurementsEvent.Listener>() {
                @Override
                public void execute(GpsMeasurementsEvent.Listener listener) throws RemoteException {
                    listener.onStatusChanged(status);
                }
            };
            GpsMeasurementListenerTransport.this.foreach(operation);
        }
    }
}
