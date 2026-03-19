package android.location;

import android.content.Context;
import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.location.LocalListenerHelper;
import android.os.RemoteException;

class GnssNavigationMessageCallbackTransport extends LocalListenerHelper<GnssNavigationMessage.Callback> {
    private final IGnssNavigationMessageListener mListenerTransport;
    private final ILocationManager mLocationManager;

    public GnssNavigationMessageCallbackTransport(Context context, ILocationManager iLocationManager) {
        super(context, "GnssNavigationMessageCallbackTransport");
        this.mListenerTransport = new ListenerTransport();
        this.mLocationManager = iLocationManager;
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
        private ListenerTransport() {
        }

        @Override
        public void onGnssNavigationMessageReceived(final GnssNavigationMessage gnssNavigationMessage) {
            GnssNavigationMessageCallbackTransport.this.foreach(new LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback>() {
                @Override
                public void execute(GnssNavigationMessage.Callback callback) throws RemoteException {
                    callback.onGnssNavigationMessageReceived(gnssNavigationMessage);
                }
            });
        }

        @Override
        public void onStatusChanged(final int i) {
            GnssNavigationMessageCallbackTransport.this.foreach(new LocalListenerHelper.ListenerOperation<GnssNavigationMessage.Callback>() {
                @Override
                public void execute(GnssNavigationMessage.Callback callback) throws RemoteException {
                    callback.onStatusChanged(i);
                }
            });
        }
    }
}
