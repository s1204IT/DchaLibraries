package android.service.trust;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.trust.ITrustAgentService;
import android.util.Log;
import android.util.Slog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@SystemApi
public class TrustAgentService extends Service {
    private static final boolean DEBUG = false;
    private static final String EXTRA_TOKEN = "token";
    private static final String EXTRA_TOKEN_HANDLE = "token_handle";
    private static final String EXTRA_TOKEN_REMOVED_RESULT = "token_removed_result";
    private static final String EXTRA_TOKEN_STATE = "token_state";
    private static final String EXTRA_USER_HANDLE = "user_handle";
    public static final int FLAG_GRANT_TRUST_DISMISS_KEYGUARD = 2;
    public static final int FLAG_GRANT_TRUST_INITIATED_BY_USER = 1;
    private static final int MSG_CONFIGURE = 2;
    private static final int MSG_DEVICE_LOCKED = 4;
    private static final int MSG_DEVICE_UNLOCKED = 5;
    private static final int MSG_ESCROW_TOKEN_ADDED = 7;
    private static final int MSG_ESCROW_TOKEN_REMOVED = 9;
    private static final int MSG_ESCROW_TOKEN_STATE_RECEIVED = 8;
    private static final int MSG_TRUST_TIMEOUT = 3;
    private static final int MSG_UNLOCK_ATTEMPT = 1;
    private static final int MSG_UNLOCK_LOCKOUT = 6;
    public static final String SERVICE_INTERFACE = "android.service.trust.TrustAgentService";
    public static final int TOKEN_STATE_ACTIVE = 1;
    public static final int TOKEN_STATE_INACTIVE = 0;
    public static final String TRUST_AGENT_META_DATA = "android.service.trust.trustagent";
    private ITrustAgentServiceCallback mCallback;
    private boolean mManagingTrust;
    private Runnable mPendingGrantTrustTask;
    private final String TAG = TrustAgentService.class.getSimpleName() + "[" + getClass().getSimpleName() + "]";
    private final Object mLock = new Object();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    TrustAgentService.this.onUnlockAttempt(message.arg1 != 0);
                    return;
                case 2:
                    ConfigurationData configurationData = (ConfigurationData) message.obj;
                    boolean zOnConfigure = TrustAgentService.this.onConfigure(configurationData.options);
                    if (configurationData.token != null) {
                        try {
                            synchronized (TrustAgentService.this.mLock) {
                                TrustAgentService.this.mCallback.onConfigureCompleted(zOnConfigure, configurationData.token);
                                break;
                            }
                            return;
                        } catch (RemoteException e) {
                            TrustAgentService.this.onError("calling onSetTrustAgentFeaturesEnabledCompleted()");
                            return;
                        }
                    }
                    return;
                case 3:
                    TrustAgentService.this.onTrustTimeout();
                    return;
                case 4:
                    TrustAgentService.this.onDeviceLocked();
                    return;
                case 5:
                    TrustAgentService.this.onDeviceUnlocked();
                    return;
                case 6:
                    TrustAgentService.this.onDeviceUnlockLockout(message.arg1);
                    return;
                case 7:
                    Bundle data = message.getData();
                    TrustAgentService.this.onEscrowTokenAdded(data.getByteArray("token"), data.getLong(TrustAgentService.EXTRA_TOKEN_HANDLE), (UserHandle) data.getParcelable(TrustAgentService.EXTRA_USER_HANDLE));
                    return;
                case 8:
                    Bundle data2 = message.getData();
                    TrustAgentService.this.onEscrowTokenStateReceived(data2.getLong(TrustAgentService.EXTRA_TOKEN_HANDLE), data2.getInt(TrustAgentService.EXTRA_TOKEN_STATE, 0));
                    return;
                case 9:
                    Bundle data3 = message.getData();
                    TrustAgentService.this.onEscrowTokenRemoved(data3.getLong(TrustAgentService.EXTRA_TOKEN_HANDLE), data3.getBoolean(TrustAgentService.EXTRA_TOKEN_REMOVED_RESULT));
                    return;
                default:
                    return;
            }
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface GrantTrustFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TokenState {
    }

    private static final class ConfigurationData {
        final List<PersistableBundle> options;
        final IBinder token;

        ConfigurationData(List<PersistableBundle> list, IBinder iBinder) {
            this.options = list;
            this.token = iBinder;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ComponentName componentName = new ComponentName(this, getClass());
        try {
            if (!Manifest.permission.BIND_TRUST_AGENT.equals(getPackageManager().getServiceInfo(componentName, 0).permission)) {
                throw new IllegalStateException(componentName.flattenToShortString() + " is not declared with the permission \"" + Manifest.permission.BIND_TRUST_AGENT + "\"");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.TAG, "Can't get ServiceInfo for " + componentName.toShortString());
        }
    }

    public void onUnlockAttempt(boolean z) {
    }

    public void onTrustTimeout() {
    }

    public void onDeviceLocked() {
    }

    public void onDeviceUnlocked() {
    }

    public void onDeviceUnlockLockout(long j) {
    }

    public void onEscrowTokenAdded(byte[] bArr, long j, UserHandle userHandle) {
    }

    public void onEscrowTokenStateReceived(long j, int i) {
    }

    public void onEscrowTokenRemoved(long j, boolean z) {
    }

    private void onError(String str) {
        Slog.v(this.TAG, "Remote exception while " + str);
    }

    public boolean onConfigure(List<PersistableBundle> list) {
        return false;
    }

    @Deprecated
    public final void grantTrust(CharSequence charSequence, long j, boolean z) {
        grantTrust(charSequence, j, z ? 1 : 0);
    }

    public final void grantTrust(final CharSequence charSequence, final long j, final int i) {
        synchronized (this.mLock) {
            if (!this.mManagingTrust) {
                throw new IllegalStateException("Cannot grant trust if agent is not managing trust. Call setManagingTrust(true) first.");
            }
            if (this.mCallback != null) {
                try {
                    this.mCallback.grantTrust(charSequence.toString(), j, i);
                } catch (RemoteException e) {
                    onError("calling enableTrust()");
                }
            } else {
                this.mPendingGrantTrustTask = new Runnable() {
                    @Override
                    public void run() {
                        TrustAgentService.this.grantTrust(charSequence, j, i);
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

    public final void setManagingTrust(boolean z) {
        synchronized (this.mLock) {
            if (this.mManagingTrust != z) {
                this.mManagingTrust = z;
                if (this.mCallback != null) {
                    try {
                        this.mCallback.setManagingTrust(z);
                    } catch (RemoteException e) {
                        onError("calling setManagingTrust()");
                    }
                }
            }
        }
    }

    public final void addEscrowToken(byte[] bArr, UserHandle userHandle) {
        synchronized (this.mLock) {
            if (this.mCallback == null) {
                Slog.w(this.TAG, "Cannot add escrow token if the agent is not connecting to framework");
                throw new IllegalStateException("Trust agent is not connected");
            }
            try {
                this.mCallback.addEscrowToken(bArr, userHandle.getIdentifier());
            } catch (RemoteException e) {
                onError("calling addEscrowToken");
            }
        }
    }

    public final void isEscrowTokenActive(long j, UserHandle userHandle) {
        synchronized (this.mLock) {
            if (this.mCallback == null) {
                Slog.w(this.TAG, "Cannot add escrow token if the agent is not connecting to framework");
                throw new IllegalStateException("Trust agent is not connected");
            }
            try {
                this.mCallback.isEscrowTokenActive(j, userHandle.getIdentifier());
            } catch (RemoteException e) {
                onError("calling isEscrowTokenActive");
            }
        }
    }

    public final void removeEscrowToken(long j, UserHandle userHandle) {
        synchronized (this.mLock) {
            if (this.mCallback == null) {
                Slog.w(this.TAG, "Cannot add escrow token if the agent is not connecting to framework");
                throw new IllegalStateException("Trust agent is not connected");
            }
            try {
                this.mCallback.removeEscrowToken(j, userHandle.getIdentifier());
            } catch (RemoteException e) {
                onError("callling removeEscrowToken");
            }
        }
    }

    public final void unlockUserWithToken(long j, byte[] bArr, UserHandle userHandle) {
        if (((UserManager) getSystemService("user")).isUserUnlocked(userHandle)) {
            Slog.i(this.TAG, "User already unlocked");
            return;
        }
        synchronized (this.mLock) {
            if (this.mCallback == null) {
                Slog.w(this.TAG, "Cannot add escrow token if the agent is not connecting to framework");
                throw new IllegalStateException("Trust agent is not connected");
            }
            try {
                this.mCallback.unlockUserWithToken(j, bArr, userHandle.getIdentifier());
            } catch (RemoteException e) {
                onError("calling unlockUserWithToken");
            }
        }
    }

    public final void showKeyguardErrorMessage(CharSequence charSequence) {
        if (charSequence == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        synchronized (this.mLock) {
            if (this.mCallback == null) {
                Slog.w(this.TAG, "Cannot show message because service is not connected to framework.");
                throw new IllegalStateException("Trust agent is not connected");
            }
            try {
                this.mCallback.showKeyguardErrorMessage(charSequence);
            } catch (RemoteException e) {
                onError("calling showKeyguardErrorMessage");
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
        public void onUnlockAttempt(boolean z) {
            TrustAgentService.this.mHandler.obtainMessage(1, z ? 1 : 0, 0).sendToTarget();
        }

        @Override
        public void onUnlockLockout(int i) {
            TrustAgentService.this.mHandler.obtainMessage(6, i, 0).sendToTarget();
        }

        @Override
        public void onTrustTimeout() {
            TrustAgentService.this.mHandler.sendEmptyMessage(3);
        }

        @Override
        public void onConfigure(List<PersistableBundle> list, IBinder iBinder) {
            TrustAgentService.this.mHandler.obtainMessage(2, new ConfigurationData(list, iBinder)).sendToTarget();
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
        public void setCallback(ITrustAgentServiceCallback iTrustAgentServiceCallback) {
            synchronized (TrustAgentService.this.mLock) {
                TrustAgentService.this.mCallback = iTrustAgentServiceCallback;
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

        @Override
        public void onEscrowTokenAdded(byte[] bArr, long j, UserHandle userHandle) {
            Message messageObtainMessage = TrustAgentService.this.mHandler.obtainMessage(7);
            messageObtainMessage.getData().putByteArray("token", bArr);
            messageObtainMessage.getData().putLong(TrustAgentService.EXTRA_TOKEN_HANDLE, j);
            messageObtainMessage.getData().putParcelable(TrustAgentService.EXTRA_USER_HANDLE, userHandle);
            messageObtainMessage.sendToTarget();
        }

        @Override
        public void onTokenStateReceived(long j, int i) {
            Message messageObtainMessage = TrustAgentService.this.mHandler.obtainMessage(8);
            messageObtainMessage.getData().putLong(TrustAgentService.EXTRA_TOKEN_HANDLE, j);
            messageObtainMessage.getData().putInt(TrustAgentService.EXTRA_TOKEN_STATE, i);
            messageObtainMessage.sendToTarget();
        }

        @Override
        public void onEscrowTokenRemoved(long j, boolean z) {
            Message messageObtainMessage = TrustAgentService.this.mHandler.obtainMessage(9);
            messageObtainMessage.getData().putLong(TrustAgentService.EXTRA_TOKEN_HANDLE, j);
            messageObtainMessage.getData().putBoolean(TrustAgentService.EXTRA_TOKEN_REMOVED_RESULT, z);
            messageObtainMessage.sendToTarget();
        }
    }
}
