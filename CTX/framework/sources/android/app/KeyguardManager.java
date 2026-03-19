package android.app;

import android.annotation.SystemApi;
import android.app.trust.ITrustManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.persistentdata.IPersistentDataBlockService;
import android.util.Log;
import android.view.IOnKeyguardExitResult;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;
import java.util.Objects;

public class KeyguardManager {
    public static final String ACTION_CONFIRM_DEVICE_CREDENTIAL = "android.app.action.CONFIRM_DEVICE_CREDENTIAL";
    public static final String ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER = "android.app.action.CONFIRM_DEVICE_CREDENTIAL_WITH_USER";
    public static final String ACTION_CONFIRM_FRP_CREDENTIAL = "android.app.action.CONFIRM_FRP_CREDENTIAL";
    public static final String EXTRA_ALTERNATE_BUTTON_LABEL = "android.app.extra.ALTERNATE_BUTTON_LABEL";
    public static final String EXTRA_DESCRIPTION = "android.app.extra.DESCRIPTION";
    public static final String EXTRA_TITLE = "android.app.extra.TITLE";
    public static final int RESULT_ALTERNATE = 1;
    private static final String TAG = "KeyguardManager";
    private final Context mContext;
    private final IWindowManager mWM = WindowManagerGlobal.getWindowManagerService();
    private final IActivityManager mAm = ActivityManager.getService();
    private final ITrustManager mTrustManager = ITrustManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.TRUST_SERVICE));

    @Deprecated
    public interface OnKeyguardExitResult {
        void onKeyguardExitResult(boolean z);
    }

    public Intent createConfirmDeviceCredentialIntent(CharSequence charSequence, CharSequence charSequence2) {
        if (BenesseExtension.getDchaState() != 0 || !isDeviceSecure()) {
            return null;
        }
        Intent intent = new Intent(ACTION_CONFIRM_DEVICE_CREDENTIAL);
        intent.putExtra(EXTRA_TITLE, charSequence);
        intent.putExtra(EXTRA_DESCRIPTION, charSequence2);
        intent.setPackage(getSettingsPackageForIntent(intent));
        return intent;
    }

    public Intent createConfirmDeviceCredentialIntent(CharSequence charSequence, CharSequence charSequence2, int i) {
        if (BenesseExtension.getDchaState() != 0 || !isDeviceSecure(i)) {
            return null;
        }
        Intent intent = new Intent(ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER);
        intent.putExtra(EXTRA_TITLE, charSequence);
        intent.putExtra(EXTRA_DESCRIPTION, charSequence2);
        intent.putExtra(Intent.EXTRA_USER_ID, i);
        intent.setPackage(getSettingsPackageForIntent(intent));
        return intent;
    }

    @SystemApi
    public Intent createConfirmFactoryResetCredentialIntent(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        if (!LockPatternUtils.frpCredentialEnabled(this.mContext)) {
            Log.w(TAG, "Factory reset credentials not supported.");
            throw new UnsupportedOperationException("not supported on this device");
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            Log.e(TAG, "Factory reset credential cannot be verified after provisioning.");
            throw new IllegalStateException("must not be provisioned yet");
        }
        try {
            IPersistentDataBlockService iPersistentDataBlockServiceAsInterface = IPersistentDataBlockService.Stub.asInterface(ServiceManager.getService(Context.PERSISTENT_DATA_BLOCK_SERVICE));
            if (iPersistentDataBlockServiceAsInterface == null) {
                Log.e(TAG, "No persistent data block service");
                throw new UnsupportedOperationException("not supported on this device");
            }
            if (!iPersistentDataBlockServiceAsInterface.hasFrpCredentialHandle()) {
                Log.i(TAG, "The persistent data block does not have a factory reset credential.");
                return null;
            }
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            Intent intent = new Intent(ACTION_CONFIRM_FRP_CREDENTIAL);
            intent.putExtra(EXTRA_TITLE, charSequence);
            intent.putExtra(EXTRA_DESCRIPTION, charSequence2);
            intent.putExtra(EXTRA_ALTERNATE_BUTTON_LABEL, charSequence3);
            intent.setPackage(getSettingsPackageForIntent(intent));
            return intent;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private String getSettingsPackageForIntent(Intent intent) {
        List<ResolveInfo> listQueryIntentActivities = this.mContext.getPackageManager().queryIntentActivities(intent, 1048576);
        if (listQueryIntentActivities.size() > 0) {
            return listQueryIntentActivities.get(0).activityInfo.packageName;
        }
        return "com.android.settings";
    }

    @Deprecated
    public class KeyguardLock {
        private final String mTag;
        private final IBinder mToken = new Binder();

        KeyguardLock(String str) {
            this.mTag = str;
        }

        public void disableKeyguard() {
            try {
                KeyguardManager.this.mWM.disableKeyguard(this.mToken, this.mTag);
            } catch (RemoteException e) {
            }
        }

        public void reenableKeyguard() {
            try {
                KeyguardManager.this.mWM.reenableKeyguard(this.mToken);
            } catch (RemoteException e) {
            }
        }
    }

    public static abstract class KeyguardDismissCallback {
        public void onDismissError() {
        }

        public void onDismissSucceeded() {
        }

        public void onDismissCancelled() {
        }
    }

    KeyguardManager(Context context) throws ServiceManager.ServiceNotFoundException {
        this.mContext = context;
    }

    @Deprecated
    public KeyguardLock newKeyguardLock(String str) {
        return new KeyguardLock(str);
    }

    public boolean isKeyguardLocked() {
        try {
            return this.mWM.isKeyguardLocked();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isKeyguardSecure() {
        try {
            return this.mWM.isKeyguardSecure();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean inKeyguardRestrictedInputMode() {
        return isKeyguardLocked();
    }

    public boolean isDeviceLocked() {
        return isDeviceLocked(this.mContext.getUserId());
    }

    public boolean isDeviceLocked(int i) {
        try {
            return this.mTrustManager.isDeviceLocked(i);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isDeviceSecure() {
        return isDeviceSecure(this.mContext.getUserId());
    }

    public boolean isDeviceSecure(int i) {
        try {
            return this.mTrustManager.isDeviceSecure(i);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Deprecated
    public void dismissKeyguard(Activity activity, KeyguardDismissCallback keyguardDismissCallback, Handler handler) {
        requestDismissKeyguard(activity, keyguardDismissCallback);
    }

    public void requestDismissKeyguard(Activity activity, KeyguardDismissCallback keyguardDismissCallback) {
        requestDismissKeyguard(activity, null, keyguardDismissCallback);
    }

    @SystemApi
    public void requestDismissKeyguard(final Activity activity, CharSequence charSequence, final KeyguardDismissCallback keyguardDismissCallback) {
        try {
            this.mAm.dismissKeyguard(activity.getActivityToken(), new IKeyguardDismissCallback.Stub() {
                @Override
                public void onDismissError() throws RemoteException {
                    if (keyguardDismissCallback != null && !activity.isDestroyed()) {
                        Handler handler = activity.mHandler;
                        final KeyguardDismissCallback keyguardDismissCallback2 = keyguardDismissCallback;
                        Objects.requireNonNull(keyguardDismissCallback2);
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                keyguardDismissCallback2.onDismissError();
                            }
                        });
                    }
                }

                @Override
                public void onDismissSucceeded() throws RemoteException {
                    if (keyguardDismissCallback != null && !activity.isDestroyed()) {
                        Handler handler = activity.mHandler;
                        final KeyguardDismissCallback keyguardDismissCallback2 = keyguardDismissCallback;
                        Objects.requireNonNull(keyguardDismissCallback2);
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                keyguardDismissCallback2.onDismissSucceeded();
                            }
                        });
                    }
                }

                @Override
                public void onDismissCancelled() throws RemoteException {
                    if (keyguardDismissCallback != null && !activity.isDestroyed()) {
                        Handler handler = activity.mHandler;
                        final KeyguardDismissCallback keyguardDismissCallback2 = keyguardDismissCallback;
                        Objects.requireNonNull(keyguardDismissCallback2);
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                keyguardDismissCallback2.onDismissCancelled();
                            }
                        });
                    }
                }
            }, charSequence);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void exitKeyguardSecurely(final OnKeyguardExitResult onKeyguardExitResult) {
        try {
            this.mWM.exitKeyguardSecurely(new IOnKeyguardExitResult.Stub() {
                @Override
                public void onKeyguardExitResult(boolean z) throws RemoteException {
                    if (onKeyguardExitResult != null) {
                        onKeyguardExitResult.onKeyguardExitResult(z);
                    }
                }
            });
        } catch (RemoteException e) {
        }
    }
}
