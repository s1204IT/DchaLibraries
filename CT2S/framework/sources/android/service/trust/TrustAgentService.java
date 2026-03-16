package android.service.trust;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.service.trust.ITrustAgentService;
import android.util.Log;
import android.util.Slog;
import java.util.List;

public class TrustAgentService extends Service {
    private static final boolean DEBUG = false;
    private static final int MSG_CONFIGURE = 2;
    private static final int MSG_DEVICE_LOCKED = 4;
    private static final int MSG_DEVICE_UNLOCKED = 5;
    private static final int MSG_TRUST_TIMEOUT = 3;
    private static final int MSG_UNLOCK_ATTEMPT = 1;
    public static final String SERVICE_INTERFACE = "android.service.trust.TrustAgentService";
    public static final String TRUST_AGENT_META_DATA = "android.service.trust.trustagent";
    private ITrustAgentServiceCallback mCallback;
    private boolean mManagingTrust;
    private Runnable mPendingGrantTrustTask;
    private final String TAG = TrustAgentService.class.getSimpleName() + "[" + getClass().getSimpleName() + "]";
    private final Object mLock = new Object();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    TrustAgentService.this.onUnlockAttempt(msg.arg1 != 0);
                    break;
                case 2:
                    ConfigurationData data = (ConfigurationData) msg.obj;
                    boolean result = TrustAgentService.this.onConfigure(data.options);
                    try {
                        synchronized (TrustAgentService.this.mLock) {
                            TrustAgentService.this.mCallback.onConfigureCompleted(result, data.token);
                            break;
                        }
                    } catch (RemoteException e) {
                        TrustAgentService.this.onError("calling onSetTrustAgentFeaturesEnabledCompleted()");
                        return;
                    }
                    break;
                case 3:
                    TrustAgentService.this.onTrustTimeout();
                    break;
                case 4:
                    TrustAgentService.this.onDeviceLocked();
                    break;
                case 5:
                    TrustAgentService.this.onDeviceUnlocked();
                    break;
            }
        }
    };

    private static final class ConfigurationData {
        final List<PersistableBundle> options;
        final IBinder token;

        ConfigurationData(List<PersistableBundle> opts, IBinder t) {
            this.options = opts;
            this.token = t;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ComponentName component = new ComponentName(this, getClass());
        try {
            ServiceInfo serviceInfo = getPackageManager().getServiceInfo(component, 0);
            if (!Manifest.permission.BIND_TRUST_AGENT.equals(serviceInfo.permission)) {
                throw new IllegalStateException(component.flattenToShortString() + " is not declared with the permission \"" + Manifest.permission.BIND_TRUST_AGENT + "\"");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.TAG, "Can't get ServiceInfo for " + component.toShortString());
        }
    }

    public void onUnlockAttempt(boolean successful) {
    }

    public void onTrustTimeout() {
    }

    public void onDeviceLocked() {
    }

    public void onDeviceUnlocked() {
    }

    private void onError(String msg) {
        Slog.v(this.TAG, "Remote exception while " + msg);
    }

    public boolean onConfigure(List<PersistableBundle> options) {
        return false;
    }

    public final void grantTrust(final CharSequence message, final long durationMs, final boolean initiatedByUser) {
        synchronized (this.mLock) {
            if (!this.mManagingTrust) {
                throw new IllegalStateException("Cannot grant trust if agent is not managing trust. Call setManagingTrust(true) first.");
            }
            if (this.mCallback != null) {
                try {
                    this.mCallback.grantTrust(message.toString(), durationMs, initiatedByUser);
                } catch (RemoteException e) {
                    onError("calling enableTrust()");
                }
            } else {
                this.mPendingGrantTrustTask = new Runnable() {
                    @Override
                    public void run() {
                        TrustAgentService.this.grantTrust(message, durationMs, initiatedByUser);
                    }
                };
            }
        }
    }

    public final void revokeTrust() {
        synchronized (this.mLock) {
            if (this.mPendingGrantTrustTask != null) {
                this.mPendingGrantTrustTask = null;
            }
            if (this.mCallback != null) {
                try {
                    this.mCallback.revokeTrust();
                } catch (RemoteException e) {
                    onError("calling revokeTrust()");
                }
            }
        }
    }

    public final void setManagingTrust(boolean managingTrust) {
        synchronized (this.mLock) {
            if (this.mManagingTrust != managingTrust) {
                this.mManagingTrust = managingTrust;
                if (this.mCallback != null) {
                    try {
                        this.mCallback.setManagingTrust(managingTrust);
                    } catch (RemoteException e) {
                        onError("calling setManagingTrust()");
                    }
                }
            }
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new TrustAgentServiceWrapper();
    }

    private final class TrustAgentServiceWrapper extends ITrustAgentService.Stub {
        private TrustAgentServiceWrapper() {
        }

        @Override
        public void onUnlockAttempt(boolean successful) {
            TrustAgentService.this.mHandler.obtainMessage(1, successful ? 1 : 0, 0).sendToTarget();
        }

        @Override
        public void onTrustTimeout() {
            TrustAgentService.this.mHandler.sendEmptyMessage(3);
        }

        @Override
        public void onConfigure(List<PersistableBundle> args, IBinder token) {
            TrustAgentService.this.mHandler.obtainMessage(2, new ConfigurationData(args, token)).sendToTarget();
        }

        @Override
        public void onDeviceLocked() throws RemoteException {
            TrustAgentService.this.mHandler.obtainMessage(4).sendToTarget();
        }

        @Override
        public void onDeviceUnlocked() throws RemoteException {
            TrustAgentService.this.mHandler.obtainMessage(5).sendToTarget();
        }

        @Override
        public void setCallback(ITrustAgentServiceCallback callback) {
            synchronized (TrustAgentService.this.mLock) {
                TrustAgentService.this.mCallback = callback;
                if (TrustAgentService.this.mManagingTrust) {
                    try {
                        TrustAgentService.this.mCallback.setManagingTrust(TrustAgentService.this.mManagingTrust);
                    } catch (RemoteException e) {
                        TrustAgentService.this.onError("calling setManagingTrust()");
                    }
                    if (TrustAgentService.this.mPendingGrantTrustTask != null) {
                        TrustAgentService.this.mPendingGrantTrustTask.run();
                        TrustAgentService.this.mPendingGrantTrustTask = null;
                    }
                } else if (TrustAgentService.this.mPendingGrantTrustTask != null) {
                }
            }
        }
    }
}
