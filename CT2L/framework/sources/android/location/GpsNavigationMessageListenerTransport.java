package android.location;

import android.content.Context;
import android.location.GpsNavigationMessageEvent;
import android.location.IGpsNavigationMessageListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GpsNavigationMessageListenerTransport extends LocalListenerHelper<GpsNavigationMessageEvent.Listener> {
    private final IGpsNavigationMessageListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GpsNavigationMessageListenerTransport(Context context, ILocationManager locationManager) {
        super(context, "GpsNavigationMessageListenerTransport");
        this.mListenerTransport = new ListenerTransport();
        this.mLocationManager = locationManager;
    }

    @Override
    protected boolean registerWithServer() throws RemoteException {
        return this.mLocationManager.addGpsNavigationMessageListener(this.mListenerTransport, getContext().getPackageName());
    }

    @Override
    protected void unregisterFromServer() throws RemoteException {
        this.mLocationManager.removeGpsNavigationMessageListener(this.mListenerTransport);
    }

    private class ListenerTransport extends IGpsNavigationMessageListener.Stub {
        private ListenerTransport() {
        }

        @Override
        public void onGpsNavigationMessageReceived(final GpsNavigationMessageEvent event) {
            LocalListenerHelper.ListenerOperation<GpsNavigationMessageEvent.Listener> operation = new LocalListenerHelper.ListenerOperation<GpsNavigationMessageEvent.Listener>() {
                @Override
                public void execute(GpsNavigationMessageEvent.Listener listener) throws RemoteException {
                    listener.onGpsNavigationMessageReceived(event);
                }
            };
            GpsNavigationMessageListenerTransport.this.foreach(operation);
        }

        @Override
        public void onStatusChanged(final int status) {
            LocalListenerHelper.ListenerOperation<GpsNavigationMessageEvent.Listener> operation = new LocalListenerHelper.ListenerOperation<GpsNavigationMessageEvent.Listener>() {
                @Override
                public void execute(GpsNavigationMessageEvent.Listener listener) throws RemoteException {
                    listener.onStatusChanged(status);
                }
            };
            GpsNavigationMessageListenerTransport.this.foreach(operation);
        }
    }
}
