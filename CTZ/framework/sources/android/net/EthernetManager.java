package android.net;

import android.content.Context;
import android.net.IEthernetServiceListener;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

public class EthernetManager {
    private static final int MSG_AVAILABILITY_CHANGED = 1000;
    private static final String TAG = "EthernetManager";
    private final Context mContext;
    private final IEthernetManager mService;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1000) {
                boolean z = message.arg1 == 1;
                Iterator it = EthernetManager.this.mListeners.iterator();
                while (it.hasNext()) {
                    ((Listener) it.next()).onAvailabilityChanged((String) message.obj, z);
                }
            }
        }
    };
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private final IEthernetServiceListener.Stub mServiceListener = new IEthernetServiceListener.Stub() {
        @Override
        public void onAvailabilityChanged(String str, boolean z) {
            EthernetManager.this.mHandler.obtainMessage(1000, z ? 1 : 0, 0, str).sendToTarget();
        }
    };

    public interface Listener {
        void onAvailabilityChanged(String str, boolean z);
    }

    public EthernetManager(Context context, IEthernetManager iEthernetManager) {
        this.mContext = context;
        this.mService = iEthernetManager;
    }

    public IpConfiguration getConfiguration(String str) {
        try {
            return this.mService.getConfiguration(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setConfiguration(String str, IpConfiguration ipConfiguration) {
        try {
            this.mService.setConfiguration(str, ipConfiguration);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAvailable() {
        return getAvailableInterfaces().length > 0;
    }

    public boolean isAvailable(String str) {
        try {
            return this.mService.isAvailable(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.mListeners.add(listener);
        if (this.mListeners.size() == 1) {
            try {
                this.mService.addListener(this.mServiceListener);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String[] getAvailableInterfaces() {
        try {
            return this.mService.getAvailableInterfaces();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void removeListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.mListeners.remove(listener);
        if (this.mListeners.isEmpty()) {
            try {
                this.mService.removeListener(this.mServiceListener);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
