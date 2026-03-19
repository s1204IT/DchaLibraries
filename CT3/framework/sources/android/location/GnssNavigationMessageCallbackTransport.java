package android.location;

import android.content.Context;
import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GnssNavigationMessageCallbackTransport extends LocalListenerHelper<GnssNavigationMessage.Callback> {
    private final IGnssNavigationMessageListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GnssNavigationMessageCallbackTransport(Context context, ILocationManager locationManager) {
        super(context, "GnssNavigationMessageCallbackTransport");
        this.mListenerTransport = new ListenerTransport(this, null);
        this.mLocationManager = locationManager;
    }

    @Override
    protected boolean registerWithServer() throws RemoteException {
        return this.mLocationManager.addGnssNavigationMessageListener(this.mListenerTransport, getContext().getPackageName());
    }

    @Override
    protected void unregisterFromServer() throws RemoteException {
        this.mLocationManager.removeGnssNavigationMessageListener(this.mListenerTransport);
    }

    private class ListenerTransport extends IGnssNavigationMessageListener.Stub {
        ListenerTransport(GnssNavigationMessageCallbackTransport this$0, ListenerTransport listenerTransport) {
            this();
        }

        private ListenerTransport() {
        }

        @Override
        public void onGnssNavigationMessageReceived(final GnssNavigationMessage event) {
            LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback> operation = new LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback>() {
                @Override
                public void execute(GnssNavigationMessage.Callback callback) throws RemoteException {
                    callback.onGnssNavigationMessageReceived(event);
                }
            };
            GnssNavigationMessageCallbackTransport.this.foreach(operation);
        }

        @Override
        public void onStatusChanged(final int status) {
            LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback> operation = new LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback>() {
                @Override
                public void execute(GnssNavigationMessage.Callback callback) throws RemoteException {
                    callback.onStatusChanged(status);
                }
            };
            GnssNavigationMessageCallbackTransport.this.foreach(operation);
        }
    }
}
