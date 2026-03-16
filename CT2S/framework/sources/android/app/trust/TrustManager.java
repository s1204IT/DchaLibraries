package android.app.trust;

import android.app.trust.ITrustListener;
import android.app.trust.ITrustManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;

public class TrustManager {
    private static final String DATA_INITIATED_BY_USER = "initiatedByUser";
    private static final int MSG_TRUST_CHANGED = 1;
    private static final int MSG_TRUST_MANAGED_CHANGED = 2;
    private static final String TAG = "TrustManager";
    private final ITrustManager mService;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean initiatedByUser = msg.peekData() != null && msg.peekData().getBoolean(TrustManager.DATA_INITIATED_BY_USER);
                    ((TrustListener) msg.obj).onTrustChanged(msg.arg1 != 0, msg.arg2, initiatedByUser);
                    break;
                case 2:
                    ((TrustListener) msg.obj).onTrustManagedChanged(msg.arg1 != 0, msg.arg2);
                    break;
            }
        }
    };
    private final ArrayMap<TrustListener, ITrustListener> mTrustListeners = new ArrayMap<>();

    public interface TrustListener {
        void onTrustChanged(boolean z, int i, boolean z2);

        void onTrustManagedChanged(boolean z, int i);
    }

    public TrustManager(IBinder b) {
        this.mService = ITrustManager.Stub.asInterface(b);
    }

    public void reportUnlockAttempt(boolean successful, int userId) {
        try {
            this.mService.reportUnlockAttempt(successful, userId);
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void reportEnabledTrustAgentsChanged(int userId) {
        try {
            this.mService.reportEnabledTrustAgentsChanged(userId);
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void reportRequireCredentialEntry(int userId) {
        try {
            this.mService.reportRequireCredentialEntry(userId);
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void reportKeyguardShowingChanged() {
        try {
            this.mService.reportKeyguardShowingChanged();
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void registerTrustListener(final TrustListener trustListener) {
        try {
            ITrustListener.Stub iTrustListener = new ITrustListener.Stub() {
                @Override
                public void onTrustChanged(boolean enabled, int userId, boolean initiatedByUser) {
                    Message m = TrustManager.this.mHandler.obtainMessage(1, enabled ? 1 : 0, userId, trustListener);
                    if (initiatedByUser) {
                        m.getData().putBoolean(TrustManager.DATA_INITIATED_BY_USER, initiatedByUser);
                    }
                    m.sendToTarget();
                }

                @Override
                public void onTrustManagedChanged(boolean managed, int userId) {
                    TrustManager.this.mHandler.obtainMessage(2, managed ? 1 : 0, userId, trustListener).sendToTarget();
                }
            };
            this.mService.registerTrustListener(iTrustListener);
            this.mTrustListeners.put(trustListener, iTrustListener);
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void unregisterTrustListener(TrustListener trustListener) {
        ITrustListener iTrustListener = this.mTrustListeners.remove(trustListener);
        if (iTrustListener != null) {
            try {
                this.mService.unregisterTrustListener(iTrustListener);
            } catch (RemoteException e) {
                onError(e);
            }
        }
    }

    private void onError(Exception e) {
        Log.e(TAG, "Error while calling TrustManagerService", e);
    }
}
