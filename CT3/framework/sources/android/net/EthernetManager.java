package android.net;

import android.content.Context;
import android.net.IEthernetServiceListener;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import java.util.ArrayList;

public class EthernetManager {
    private static final int MSG_AVAILABILITY_CHANGED = 1000;
    private static final String TAG = "EthernetManager";
    private final Context mContext;
    private final IEthernetManager mService;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 1000) {
                return;
            }
            boolean isAvailable = msg.arg1 == 1;
            for (Listener listener : EthernetManager.this.mListeners) {
                listener.onAvailabilityChanged(isAvailable);
            }
        }
    };
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private final IEthernetServiceListener.Stub mServiceListener = new IEthernetServiceListener.Stub() {
        @Override
        public void onAvailabilityChanged(boolean isAvailable) {
            EthernetManager.this.mHandler.obtainMessage(1000, isAvailable ? 1 : 0, 0, null).sendToTarget();
        }
    };

    public interface Listener {
        void onAvailabilityChanged(boolean z);
    }

    public EthernetManager(Context context, IEthernetManager service) {
        this.mContext = context;
        this.mService = service;
    }

    public IpConfiguration getConfiguration() {
        try {
            return this.mService.getConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setConfiguration(IpConfiguration config) {
        try {
            this.mService.setConfiguration(config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAvailable() {
        try {
            return this.mService.isAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.mListeners.add(listener);
        if (this.mListeners.size() != 1) {
            return;
        }
        try {
            this.mService.addListener(this.mServiceListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.mListeners.remove(listener);
        if (!this.mListeners.isEmpty()) {
            return;
        }
        try {
            this.mService.removeListener(this.mServiceListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
