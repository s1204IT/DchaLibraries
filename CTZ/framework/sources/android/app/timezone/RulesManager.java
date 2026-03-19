package android.app.timezone;

import android.app.timezone.ICallback;
import android.app.timezone.IRulesManager;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class RulesManager {
    public static final String ACTION_RULES_UPDATE_OPERATION = "com.android.intent.action.timezone.RULES_UPDATE_OPERATION";
    private static final boolean DEBUG = false;
    public static final int ERROR_OPERATION_IN_PROGRESS = 1;
    public static final int ERROR_UNKNOWN_FAILURE = 2;
    public static final String EXTRA_OPERATION_STAGED = "staged";
    public static final int SUCCESS = 0;
    private static final String TAG = "timezone.RulesManager";
    private final Context mContext;
    private final IRulesManager mIRulesManager = IRulesManager.Stub.asInterface(ServiceManager.getService(Context.TIME_ZONE_RULES_MANAGER_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {
    }

    public RulesManager(Context context) {
        this.mContext = context;
    }

    public RulesState getRulesState() {
        try {
            logDebug("mIRulesManager.getRulesState()");
            RulesState rulesState = this.mIRulesManager.getRulesState();
            logDebug("mIRulesManager.getRulesState() returned " + rulesState);
            return rulesState;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestInstall(ParcelFileDescriptor parcelFileDescriptor, byte[] bArr, Callback callback) throws IOException {
        CallbackWrapper callbackWrapper = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestInstall()");
            return this.mIRulesManager.requestInstall(parcelFileDescriptor, bArr, callbackWrapper);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestUninstall(byte[] bArr, Callback callback) {
        CallbackWrapper callbackWrapper = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestUninstall()");
            return this.mIRulesManager.requestUninstall(bArr, callbackWrapper);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private class CallbackWrapper extends ICallback.Stub {
        final Callback mCallback;
        final Handler mHandler;

        CallbackWrapper(Context context, Callback callback) {
            this.mCallback = callback;
            this.mHandler = new Handler(context.getMainLooper());
        }

        @Override
        public void onFinished(final int i) {
            RulesManager.logDebug("mCallback.onFinished(status), status=" + i);
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onFinished(i);
                }
            });
        }
    }

    public void requestNothing(byte[] bArr, boolean z) {
        try {
            logDebug("mIRulesManager.requestNothing() with token=" + Arrays.toString(bArr));
            this.mIRulesManager.requestNothing(bArr, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    static void logDebug(String str) {
    }
}
