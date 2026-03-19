package android.location;

import android.content.Context;
import android.location.IBatchedLocationCallback;
import android.location.LocalListenerHelper;
import android.os.RemoteException;
import java.util.List;

class BatchedLocationCallbackTransport extends LocalListenerHelper<BatchedLocationCallback> {
    private final IBatchedLocationCallback mCallbackTransport;
    private final ILocationManager mLocationManager;

    public BatchedLocationCallbackTransport(Context context, ILocationManager iLocationManager) {
        super(context, "BatchedLocationCallbackTransport");
        this.mCallbackTransport = new CallbackTransport();
        this.mLocationManager = iLocationManager;
    }

    @Override
    protected boolean registerWithServer() throws RemoteException {
        return this.mLocationManager.addGnssBatchingCallback(this.mCallbackTransport, getContext().getPackageName());
    }

    @Override
    protected void unregisterFromServer() throws RemoteException {
        this.mLocationManager.removeGnssBatchingCallback();
    }

    private class CallbackTransport extends IBatchedLocationCallback.Stub {
        private CallbackTransport() {
        }

        @Override
        public void onLocationBatch(final List<Location> list) {
            BatchedLocationCallbackTransport.this.foreach(new LocalListenerHelper.ListenerOperation<BatchedLocationCallback>() {
                @Override
                public void execute(BatchedLocationCallback batchedLocationCallback) throws RemoteException {
                    batchedLocationCallback.onLocationBatch(list);
                }
            });
        }
    }
}
