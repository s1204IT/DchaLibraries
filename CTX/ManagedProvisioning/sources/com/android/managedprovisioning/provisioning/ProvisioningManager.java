package com.android.managedprovisioning.provisioning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.analytics.TimeLogger;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.EncryptionController;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProvisioningManager implements ProvisioningControllerCallback {
    private static final Intent SERVICE_INTENT = new Intent().setComponent(new ComponentName("com.android.managedprovisioning", ProvisioningService.class.getName()));
    private static ProvisioningManager sInstance;

    @GuardedBy("this")
    private List<ProvisioningManagerCallback> mCallbacks;
    private final Context mContext;

    @GuardedBy("this")
    private AbstractProvisioningController mController;
    private final ProvisioningControllerFactory mFactory;
    private HandlerThread mHandlerThread;
    private int mLastCallback;
    private Pair<Pair<Integer, Integer>, Boolean> mLastError;
    private int mLastProgressMsgId;
    private final ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker;
    private final TimeLogger mTimeLogger;
    private final Handler mUiHandler;

    public static ProvisioningManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProvisioningManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private ProvisioningManager(Context context) {
        this(context, new Handler(Looper.getMainLooper()), new ProvisioningControllerFactory(), ProvisioningAnalyticsTracker.getInstance(), new TimeLogger(context, 627));
    }

    @VisibleForTesting
    ProvisioningManager(Context context, Handler handler, ProvisioningControllerFactory provisioningControllerFactory, ProvisioningAnalyticsTracker provisioningAnalyticsTracker, TimeLogger timeLogger) {
        this.mCallbacks = new ArrayList();
        this.mLastCallback = 0;
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUiHandler = (Handler) Preconditions.checkNotNull(handler);
        this.mFactory = (ProvisioningControllerFactory) Preconditions.checkNotNull(provisioningControllerFactory);
        this.mProvisioningAnalyticsTracker = (ProvisioningAnalyticsTracker) Preconditions.checkNotNull(provisioningAnalyticsTracker);
        this.mTimeLogger = (TimeLogger) Preconditions.checkNotNull(timeLogger);
    }

    public void maybeStartProvisioning(ProvisioningParams provisioningParams) {
        synchronized (this) {
            if (this.mController == null) {
                this.mTimeLogger.start();
                startNewProvisioningLocked(provisioningParams);
                this.mProvisioningAnalyticsTracker.logProvisioningStarted(this.mContext, provisioningParams);
            } else {
                ProvisionLogger.loge("Trying to start provisioning, but it's already running");
            }
        }
    }

    private void startNewProvisioningLocked(ProvisioningParams provisioningParams) {
        ProvisionLogger.logd("Initializing provisioning process");
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread("Provisioning Worker");
            this.mHandlerThread.start();
            this.mContext.startService(SERVICE_INTENT);
        }
        this.mLastCallback = 0;
        this.mLastError = null;
        this.mLastProgressMsgId = 0;
        this.mController = this.mFactory.createProvisioningController(this.mContext, provisioningParams, this);
        this.mController.start(this.mHandlerThread.getLooper());
    }

    public void cancelProvisioning() {
        synchronized (this) {
            if (this.mController != null) {
                this.mProvisioningAnalyticsTracker.logProvisioningCancelled(this.mContext, 2);
                this.mController.cancel();
            } else {
                ProvisionLogger.loge("Trying to cancel provisioning, but controller is null");
            }
        }
    }

    public void registerListener(ProvisioningManagerCallback provisioningManagerCallback) {
        synchronized (this) {
            this.mCallbacks.add(provisioningManagerCallback);
            callLastCallbackLocked(provisioningManagerCallback);
        }
    }

    public void unregisterListener(ProvisioningManagerCallback provisioningManagerCallback) {
        synchronized (this) {
            this.mCallbacks.remove(provisioningManagerCallback);
        }
    }

    @Override
    public void cleanUpCompleted() {
        synchronized (this) {
            clearControllerLocked();
        }
    }

    @Override
    public void error(final int i, final int i2, final boolean z) {
        synchronized (this) {
            for (final ProvisioningManagerCallback provisioningManagerCallback : this.mCallbacks) {
                this.mUiHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        provisioningManagerCallback.error(i, i2, z);
                    }
                });
            }
            this.mLastCallback = 1;
            this.mLastError = Pair.create(Pair.create(Integer.valueOf(i), Integer.valueOf(i2)), Boolean.valueOf(z));
        }
    }

    @Override
    public void progressUpdate(final int i) {
        synchronized (this) {
            for (final ProvisioningManagerCallback provisioningManagerCallback : this.mCallbacks) {
                this.mUiHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        provisioningManagerCallback.progressUpdate(i);
                    }
                });
            }
            this.mLastCallback = 2;
            this.mLastProgressMsgId = i;
        }
    }

    @Override
    public void provisioningTasksCompleted() {
        synchronized (this) {
            this.mTimeLogger.stop();
            if (this.mController != null) {
                Handler handler = this.mUiHandler;
                final AbstractProvisioningController abstractProvisioningController = this.mController;
                Objects.requireNonNull(abstractProvisioningController);
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        abstractProvisioningController.preFinalize();
                    }
                });
            } else {
                ProvisionLogger.loge("Trying to pre-finalize provisioning, but controller is null");
            }
        }
    }

    @Override
    public void preFinalizationCompleted() {
        synchronized (this) {
            for (ProvisioningManagerCallback provisioningManagerCallback : this.mCallbacks) {
                Handler handler = this.mUiHandler;
                Objects.requireNonNull(provisioningManagerCallback);
                handler.post(new $$Lambda$LjHuY_MfhVzzFPTipfFDu9wWFLY(provisioningManagerCallback));
            }
            this.mLastCallback = 3;
            this.mProvisioningAnalyticsTracker.logProvisioningSessionCompleted(this.mContext);
            clearControllerLocked();
            ProvisionLogger.logi("ProvisioningManager pre-finalization completed");
        }
    }

    private void callLastCallbackLocked(final ProvisioningManagerCallback provisioningManagerCallback) {
        switch (this.mLastCallback) {
            case EncryptionController.NOTIFICATION_ID:
                final Pair<Pair<Integer, Integer>, Boolean> pair = this.mLastError;
                this.mUiHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ProvisioningManagerCallback provisioningManagerCallback2 = provisioningManagerCallback;
                        Pair pair2 = pair;
                        provisioningManagerCallback2.error(((Integer) ((Pair) pair2.first).first).intValue(), ((Integer) ((Pair) pair2.first).second).intValue(), ((Boolean) pair2.second).booleanValue());
                    }
                });
                break;
            case 2:
                final int i = this.mLastProgressMsgId;
                this.mUiHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        provisioningManagerCallback.progressUpdate(i);
                    }
                });
                break;
            case 3:
                Handler handler = this.mUiHandler;
                Objects.requireNonNull(provisioningManagerCallback);
                handler.post(new $$Lambda$LjHuY_MfhVzzFPTipfFDu9wWFLY(provisioningManagerCallback));
                break;
            default:
                ProvisionLogger.logd("No previous callback");
                break;
        }
    }

    private void clearControllerLocked() {
        this.mController = null;
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
            this.mContext.stopService(SERVICE_INTENT);
        }
    }
}
