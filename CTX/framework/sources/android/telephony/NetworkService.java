package android.telephony;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.INetworkService;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class NetworkService extends Service {
    private static final int NETWORK_SERVICE_CREATE_NETWORK_SERVICE_PROVIDER = 1;
    public static final String NETWORK_SERVICE_EXTRA_SLOT_ID = "android.telephony.extra.SLOT_ID";
    private static final int NETWORK_SERVICE_GET_REGISTRATION_STATE = 4;
    private static final int NETWORK_SERVICE_INDICATION_NETWORK_STATE_CHANGED = 7;
    public static final String NETWORK_SERVICE_INTERFACE = "android.telephony.NetworkService";
    private static final int NETWORK_SERVICE_REGISTER_FOR_STATE_CHANGE = 5;
    private static final int NETWORK_SERVICE_REMOVE_ALL_NETWORK_SERVICE_PROVIDERS = 3;
    private static final int NETWORK_SERVICE_REMOVE_NETWORK_SERVICE_PROVIDER = 2;
    private static final int NETWORK_SERVICE_UNREGISTER_FOR_STATE_CHANGE = 6;
    private final NetworkServiceHandler mHandler;
    private final String TAG = NetworkService.class.getSimpleName();
    private final SparseArray<NetworkServiceProvider> mServiceMap = new SparseArray<>();

    @VisibleForTesting
    public final INetworkServiceWrapper mBinder = new INetworkServiceWrapper();
    private final HandlerThread mHandlerThread = new HandlerThread(this.TAG);

    protected abstract NetworkServiceProvider createNetworkServiceProvider(int i);

    public class NetworkServiceProvider {
        private final List<INetworkServiceCallback> mNetworkRegistrationStateChangedCallbacks = new ArrayList();
        private final int mSlotId;

        public NetworkServiceProvider(int i) {
            this.mSlotId = i;
        }

        public final int getSlotId() {
            return this.mSlotId;
        }

        public void getNetworkRegistrationState(int i, NetworkServiceCallback networkServiceCallback) {
            networkServiceCallback.onGetNetworkRegistrationStateComplete(1, null);
        }

        public final void notifyNetworkRegistrationStateChanged() {
            NetworkService.this.mHandler.obtainMessage(7, this.mSlotId, 0, null).sendToTarget();
        }

        private void registerForStateChanged(INetworkServiceCallback iNetworkServiceCallback) {
            synchronized (this.mNetworkRegistrationStateChangedCallbacks) {
                this.mNetworkRegistrationStateChangedCallbacks.add(iNetworkServiceCallback);
            }
        }

        private void unregisterForStateChanged(INetworkServiceCallback iNetworkServiceCallback) {
            synchronized (this.mNetworkRegistrationStateChangedCallbacks) {
                this.mNetworkRegistrationStateChangedCallbacks.remove(iNetworkServiceCallback);
            }
        }

        private void notifyStateChangedToCallbacks() {
            Iterator<INetworkServiceCallback> it = this.mNetworkRegistrationStateChangedCallbacks.iterator();
            while (it.hasNext()) {
                try {
                    it.next().onNetworkStateChanged();
                } catch (RemoteException e) {
                }
            }
        }

        protected void onDestroy() {
            this.mNetworkRegistrationStateChangedCallbacks.clear();
        }
    }

    private class NetworkServiceHandler extends Handler {
        NetworkServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.arg1;
            INetworkServiceCallback iNetworkServiceCallback = (INetworkServiceCallback) message.obj;
            NetworkServiceProvider networkServiceProvider = (NetworkServiceProvider) NetworkService.this.mServiceMap.get(i);
            switch (message.what) {
                case 1:
                    if (networkServiceProvider == null) {
                        NetworkService.this.mServiceMap.put(i, NetworkService.this.createNetworkServiceProvider(i));
                    }
                    break;
                case 2:
                    if (networkServiceProvider != null) {
                        networkServiceProvider.onDestroy();
                        NetworkService.this.mServiceMap.remove(i);
                    }
                    break;
                case 3:
                    for (int i2 = 0; i2 < NetworkService.this.mServiceMap.size(); i2++) {
                        NetworkServiceProvider networkServiceProvider2 = (NetworkServiceProvider) NetworkService.this.mServiceMap.get(i2);
                        if (networkServiceProvider2 != null) {
                            networkServiceProvider2.onDestroy();
                        }
                    }
                    NetworkService.this.mServiceMap.clear();
                    break;
                case 4:
                    if (networkServiceProvider != null) {
                        networkServiceProvider.getNetworkRegistrationState(message.arg2, new NetworkServiceCallback(iNetworkServiceCallback));
                        break;
                    }
                    break;
                case 5:
                    if (networkServiceProvider != null) {
                        networkServiceProvider.registerForStateChanged(iNetworkServiceCallback);
                        break;
                    }
                    break;
                case 6:
                    if (networkServiceProvider != null) {
                        networkServiceProvider.unregisterForStateChanged(iNetworkServiceCallback);
                        break;
                    }
                    break;
                case 7:
                    if (networkServiceProvider != null) {
                        networkServiceProvider.notifyStateChangedToCallbacks();
                        break;
                    }
                    break;
            }
        }
    }

    public NetworkService() {
        this.mHandlerThread.start();
        this.mHandler = new NetworkServiceHandler(this.mHandlerThread.getLooper());
        log("network service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !NETWORK_SERVICE_INTERFACE.equals(intent.getAction())) {
            loge("Unexpected intent " + intent);
            return null;
        }
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mHandler.obtainMessage(3, 0, 0, null).sendToTarget();
        return false;
    }

    @Override
    public void onDestroy() {
        this.mHandlerThread.quit();
    }

    private class INetworkServiceWrapper extends INetworkService.Stub {
        private INetworkServiceWrapper() {
        }

        @Override
        public void createNetworkServiceProvider(int i) {
            NetworkService.this.mHandler.obtainMessage(1, i, 0, null).sendToTarget();
        }

        @Override
        public void removeNetworkServiceProvider(int i) {
            NetworkService.this.mHandler.obtainMessage(2, i, 0, null).sendToTarget();
        }

        @Override
        public void getNetworkRegistrationState(int i, int i2, INetworkServiceCallback iNetworkServiceCallback) {
            NetworkService.this.mHandler.obtainMessage(4, i, i2, iNetworkServiceCallback).sendToTarget();
        }

        @Override
        public void registerForNetworkRegistrationStateChanged(int i, INetworkServiceCallback iNetworkServiceCallback) {
            NetworkService.this.mHandler.obtainMessage(5, i, 0, iNetworkServiceCallback).sendToTarget();
        }

        @Override
        public void unregisterForNetworkRegistrationStateChanged(int i, INetworkServiceCallback iNetworkServiceCallback) {
            NetworkService.this.mHandler.obtainMessage(6, i, 0, iNetworkServiceCallback).sendToTarget();
        }
    }

    private final void log(String str) {
        Rlog.d(this.TAG, str);
    }

    private final void loge(String str) {
        Rlog.e(this.TAG, str);
    }
}
