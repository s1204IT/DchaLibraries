package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.IAutoFillService;
import android.service.autofill.IFillCallback;
import android.service.autofill.ISaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.autofill.RemoteFillService;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class RemoteFillService implements IBinder.DeathRecipient {
    private static final String LOG_TAG = "RemoteFillService";
    private static final int MSG_UNBIND = 3;
    private static final long TIMEOUT_IDLE_BIND_MILLIS = 5000;
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 5000;
    private IAutoFillService mAutoFillService;
    private final boolean mBindInstantServiceAllowed;
    private boolean mBinding;
    private final FillServiceCallbacks mCallbacks;
    private boolean mCompleted;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private final Intent mIntent;
    private PendingRequest mPendingRequest;
    private boolean mServiceDied;
    private final int mUserId;
    private final ServiceConnection mServiceConnection = new RemoteServiceConnection();
    private final Handler mHandler = new Handler(FgThread.getHandler().getLooper());

    public interface FillServiceCallbacks {
        void onFillRequestFailure(int i, CharSequence charSequence, String str);

        void onFillRequestSuccess(int i, FillResponse fillResponse, String str, int i2);

        void onFillRequestTimeout(int i, String str);

        void onSaveRequestFailure(CharSequence charSequence, String str);

        void onSaveRequestSuccess(String str, IntentSender intentSender);

        void onServiceDied(RemoteFillService remoteFillService);
    }

    public RemoteFillService(Context context, ComponentName componentName, int i, FillServiceCallbacks fillServiceCallbacks, boolean z) {
        this.mContext = context;
        this.mCallbacks = fillServiceCallbacks;
        this.mComponentName = componentName;
        this.mIntent = new Intent("android.service.autofill.AutofillService").setComponent(this.mComponentName);
        this.mUserId = i;
        this.mBindInstantServiceAllowed = z;
    }

    public void destroy() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemoteFillService) obj).handleDestroy();
            }
        }, this));
    }

    private void handleDestroy() {
        if (checkIfDestroyed()) {
            return;
        }
        if (this.mPendingRequest != null) {
            this.mPendingRequest.cancel();
            this.mPendingRequest = null;
        }
        ensureUnbound();
        this.mDestroyed = true;
    }

    @Override
    public void binderDied() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemoteFillService) obj).handleBinderDied();
            }
        }, this));
    }

    private void handleBinderDied() {
        if (checkIfDestroyed()) {
            return;
        }
        if (this.mAutoFillService != null) {
            this.mAutoFillService.asBinder().unlinkToDeath(this, 0);
        }
        this.mAutoFillService = null;
        this.mServiceDied = true;
        this.mCallbacks.onServiceDied(this);
    }

    public int cancelCurrentRequest() {
        if (this.mDestroyed) {
            return Integer.MIN_VALUE;
        }
        if (this.mPendingRequest != null) {
            id = this.mPendingRequest instanceof PendingFillRequest ? ((PendingFillRequest) this.mPendingRequest).mRequest.getId() : Integer.MIN_VALUE;
            this.mPendingRequest.cancel();
            this.mPendingRequest = null;
        }
        return id;
    }

    public void onFillRequest(FillRequest fillRequest) {
        cancelScheduledUnbind();
        scheduleRequest(new PendingFillRequest(fillRequest, this));
    }

    public void onSaveRequest(SaveRequest saveRequest) {
        cancelScheduledUnbind();
        scheduleRequest(new PendingSaveRequest(saveRequest, this));
    }

    private void scheduleRequest(PendingRequest pendingRequest) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((RemoteFillService) obj).handlePendingRequest((RemoteFillService.PendingRequest) obj2);
            }
        }, this, pendingRequest));
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.append((CharSequence) str).append("service:").println();
        printWriter.append((CharSequence) str).append("  ").append("userId=").append((CharSequence) String.valueOf(this.mUserId)).println();
        printWriter.append((CharSequence) str).append("  ").append("componentName=").append((CharSequence) this.mComponentName.flattenToString()).println();
        printWriter.append((CharSequence) str).append("  ").append("destroyed=").append((CharSequence) String.valueOf(this.mDestroyed)).println();
        printWriter.append((CharSequence) str).append("  ").append("bound=").append((CharSequence) String.valueOf(isBound())).println();
        printWriter.append((CharSequence) str).append("  ").append("hasPendingRequest=").append((CharSequence) String.valueOf(this.mPendingRequest != null)).println();
        printWriter.append((CharSequence) str).append("mBindInstantServiceAllowed=").println(this.mBindInstantServiceAllowed);
        printWriter.println();
    }

    private void cancelScheduledUnbind() {
        this.mHandler.removeMessages(3);
    }

    private void scheduleUnbind() {
        cancelScheduledUnbind();
        this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((RemoteFillService) obj).handleUnbind();
            }
        }, this).setWhat(3), 5000L);
    }

    private void handleUnbind() {
        if (checkIfDestroyed()) {
            return;
        }
        ensureUnbound();
    }

    private void handlePendingRequest(PendingRequest pendingRequest) {
        if (checkIfDestroyed() || this.mCompleted) {
            return;
        }
        if (!isBound()) {
            if (this.mPendingRequest != null) {
                this.mPendingRequest.cancel();
            }
            this.mPendingRequest = pendingRequest;
            ensureBound();
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(LOG_TAG, "[user: " + this.mUserId + "] handlePendingRequest()");
        }
        pendingRequest.run();
        if (pendingRequest.isFinal()) {
            this.mCompleted = true;
        }
    }

    private boolean isBound() {
        return this.mAutoFillService != null;
    }

    private void ensureBound() {
        if (isBound() || this.mBinding) {
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(LOG_TAG, "[user: " + this.mUserId + "] ensureBound()");
        }
        this.mBinding = true;
        int i = 67108865;
        if (this.mBindInstantServiceAllowed) {
            i = 71303169;
        }
        if (!this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, i, new UserHandle(this.mUserId))) {
            Slog.w(LOG_TAG, "[user: " + this.mUserId + "] could not bind to " + this.mIntent + " using flags " + i);
            this.mBinding = false;
            if (!this.mServiceDied) {
                handleBinderDied();
            }
        }
    }

    private void ensureUnbound() {
        if (!isBound() && !this.mBinding) {
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(LOG_TAG, "[user: " + this.mUserId + "] ensureUnbound()");
        }
        this.mBinding = false;
        if (isBound()) {
            try {
                this.mAutoFillService.onConnectedStateChanged(false);
            } catch (Exception e) {
                Slog.w(LOG_TAG, "Exception calling onDisconnected(): " + e);
            }
            if (this.mAutoFillService != null) {
                this.mAutoFillService.asBinder().unlinkToDeath(this, 0);
                this.mAutoFillService = null;
            }
        }
        this.mContext.unbindService(this.mServiceConnection);
    }

    private void dispatchOnFillRequestSuccess(final PendingFillRequest pendingFillRequest, final FillResponse fillResponse, final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnFillRequestSuccess$0(this.f$0, pendingFillRequest, fillResponse, i);
            }
        });
    }

    public static void lambda$dispatchOnFillRequestSuccess$0(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest, FillResponse fillResponse, int i) {
        if (remoteFillService.handleResponseCallbackCommon(pendingFillRequest)) {
            remoteFillService.mCallbacks.onFillRequestSuccess(pendingFillRequest.mRequest.getId(), fillResponse, remoteFillService.mComponentName.getPackageName(), i);
        }
    }

    private void dispatchOnFillRequestFailure(final PendingFillRequest pendingFillRequest, final CharSequence charSequence) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnFillRequestFailure$1(this.f$0, pendingFillRequest, charSequence);
            }
        });
    }

    public static void lambda$dispatchOnFillRequestFailure$1(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest, CharSequence charSequence) {
        if (remoteFillService.handleResponseCallbackCommon(pendingFillRequest)) {
            remoteFillService.mCallbacks.onFillRequestFailure(pendingFillRequest.mRequest.getId(), charSequence, remoteFillService.mComponentName.getPackageName());
        }
    }

    private void dispatchOnFillRequestTimeout(final PendingFillRequest pendingFillRequest) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnFillRequestTimeout$2(this.f$0, pendingFillRequest);
            }
        });
    }

    public static void lambda$dispatchOnFillRequestTimeout$2(RemoteFillService remoteFillService, PendingFillRequest pendingFillRequest) {
        if (remoteFillService.handleResponseCallbackCommon(pendingFillRequest)) {
            remoteFillService.mCallbacks.onFillRequestTimeout(pendingFillRequest.mRequest.getId(), remoteFillService.mComponentName.getPackageName());
        }
    }

    private void dispatchOnFillTimeout(final ICancellationSignal iCancellationSignal) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnFillTimeout$3(iCancellationSignal);
            }
        });
    }

    static void lambda$dispatchOnFillTimeout$3(ICancellationSignal iCancellationSignal) {
        try {
            iCancellationSignal.cancel();
        } catch (RemoteException e) {
            Slog.w(LOG_TAG, "Error calling cancellation signal: " + e);
        }
    }

    private void dispatchOnSaveRequestSuccess(final PendingRequest pendingRequest, final IntentSender intentSender) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnSaveRequestSuccess$4(this.f$0, pendingRequest, intentSender);
            }
        });
    }

    public static void lambda$dispatchOnSaveRequestSuccess$4(RemoteFillService remoteFillService, PendingRequest pendingRequest, IntentSender intentSender) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onSaveRequestSuccess(remoteFillService.mComponentName.getPackageName(), intentSender);
        }
    }

    private void dispatchOnSaveRequestFailure(final PendingRequest pendingRequest, final CharSequence charSequence) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.lambda$dispatchOnSaveRequestFailure$5(this.f$0, pendingRequest, charSequence);
            }
        });
    }

    public static void lambda$dispatchOnSaveRequestFailure$5(RemoteFillService remoteFillService, PendingRequest pendingRequest, CharSequence charSequence) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onSaveRequestFailure(charSequence, remoteFillService.mComponentName.getPackageName());
        }
    }

    private boolean handleResponseCallbackCommon(PendingRequest pendingRequest) {
        if (this.mDestroyed) {
            return false;
        }
        if (this.mPendingRequest == pendingRequest) {
            this.mPendingRequest = null;
        }
        if (this.mPendingRequest == null) {
            scheduleUnbind();
            return true;
        }
        return true;
    }

    private class RemoteServiceConnection implements ServiceConnection {
        private RemoteServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (!RemoteFillService.this.mDestroyed && RemoteFillService.this.mBinding) {
                RemoteFillService.this.mBinding = false;
                RemoteFillService.this.mAutoFillService = IAutoFillService.Stub.asInterface(iBinder);
                try {
                    iBinder.linkToDeath(RemoteFillService.this, 0);
                    try {
                        RemoteFillService.this.mAutoFillService.onConnectedStateChanged(true);
                    } catch (RemoteException e) {
                        Slog.w(RemoteFillService.LOG_TAG, "Exception calling onConnected(): " + e);
                    }
                    if (RemoteFillService.this.mPendingRequest != null) {
                        PendingRequest pendingRequest = RemoteFillService.this.mPendingRequest;
                        RemoteFillService.this.mPendingRequest = null;
                        RemoteFillService.this.handlePendingRequest(pendingRequest);
                    }
                    RemoteFillService.this.mServiceDied = false;
                    return;
                } catch (RemoteException e2) {
                    RemoteFillService.this.handleBinderDied();
                    return;
                }
            }
            Slog.wtf(RemoteFillService.LOG_TAG, "onServiceConnected was dispatched after unbindService.");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            RemoteFillService.this.mBinding = true;
            RemoteFillService.this.mAutoFillService = null;
        }
    }

    private boolean checkIfDestroyed() {
        if (this.mDestroyed && Helper.sVerbose) {
            Slog.v(LOG_TAG, "Not handling operation as service for " + this.mComponentName + " is already destroyed");
        }
        return this.mDestroyed;
    }

    private static abstract class PendingRequest implements Runnable {

        @GuardedBy("mLock")
        private boolean mCancelled;

        @GuardedBy("mLock")
        private boolean mCompleted;
        private final Handler mServiceHandler;
        private final WeakReference<RemoteFillService> mWeakService;
        protected final Object mLock = new Object();
        private final Runnable mTimeoutTrigger = new Runnable() {
            @Override
            public final void run() {
                RemoteFillService.PendingRequest.lambda$new$0(this.f$0);
            }
        };

        abstract void onTimeout(RemoteFillService remoteFillService);

        PendingRequest(RemoteFillService remoteFillService) {
            this.mWeakService = new WeakReference<>(remoteFillService);
            this.mServiceHandler = remoteFillService.mHandler;
            this.mServiceHandler.postAtTime(this.mTimeoutTrigger, SystemClock.uptimeMillis() + 5000);
        }

        public static void lambda$new$0(PendingRequest pendingRequest) {
            synchronized (pendingRequest.mLock) {
                if (pendingRequest.mCancelled) {
                    return;
                }
                pendingRequest.mCompleted = true;
                Slog.w(RemoteFillService.LOG_TAG, pendingRequest.getClass().getSimpleName() + " timed out");
                RemoteFillService remoteFillService = pendingRequest.mWeakService.get();
                if (remoteFillService != null) {
                    Slog.w(RemoteFillService.LOG_TAG, pendingRequest.getClass().getSimpleName() + " timed out after 5000 ms");
                    pendingRequest.onTimeout(remoteFillService);
                }
            }
        }

        protected RemoteFillService getService() {
            return this.mWeakService.get();
        }

        protected final boolean finish() {
            synchronized (this.mLock) {
                if (!this.mCompleted && !this.mCancelled) {
                    this.mCompleted = true;
                    this.mServiceHandler.removeCallbacks(this.mTimeoutTrigger);
                    return true;
                }
                return false;
            }
        }

        @GuardedBy("mLock")
        protected boolean isCancelledLocked() {
            return this.mCancelled;
        }

        boolean cancel() {
            synchronized (this.mLock) {
                if (!this.mCancelled && !this.mCompleted) {
                    this.mCancelled = true;
                    this.mServiceHandler.removeCallbacks(this.mTimeoutTrigger);
                    return true;
                }
                return false;
            }
        }

        boolean isFinal() {
            return false;
        }
    }

    private static final class PendingFillRequest extends PendingRequest {
        private final IFillCallback mCallback;
        private ICancellationSignal mCancellation;
        private final FillRequest mRequest;

        public PendingFillRequest(final FillRequest fillRequest, RemoteFillService remoteFillService) {
            super(remoteFillService);
            this.mRequest = fillRequest;
            this.mCallback = new IFillCallback.Stub() {
                public void onCancellable(ICancellationSignal iCancellationSignal) {
                    boolean zIsCancelledLocked;
                    synchronized (PendingFillRequest.this.mLock) {
                        synchronized (PendingFillRequest.this.mLock) {
                            PendingFillRequest.this.mCancellation = iCancellationSignal;
                            zIsCancelledLocked = PendingFillRequest.this.isCancelledLocked();
                        }
                        if (zIsCancelledLocked) {
                            try {
                                iCancellationSignal.cancel();
                            } catch (RemoteException e) {
                                Slog.e(RemoteFillService.LOG_TAG, "Error requesting a cancellation", e);
                            }
                        }
                    }
                }

                public void onSuccess(FillResponse fillResponse) {
                    RemoteFillService service;
                    if (PendingFillRequest.this.finish() && (service = PendingFillRequest.this.getService()) != null) {
                        service.dispatchOnFillRequestSuccess(PendingFillRequest.this, fillResponse, fillRequest.getFlags());
                    }
                }

                public void onFailure(int i, CharSequence charSequence) {
                    RemoteFillService service;
                    if (PendingFillRequest.this.finish() && (service = PendingFillRequest.this.getService()) != null) {
                        service.dispatchOnFillRequestFailure(PendingFillRequest.this, charSequence);
                    }
                }
            };
        }

        @Override
        void onTimeout(RemoteFillService remoteFillService) {
            ICancellationSignal iCancellationSignal;
            synchronized (this.mLock) {
                iCancellationSignal = this.mCancellation;
            }
            if (iCancellationSignal != null) {
                remoteFillService.dispatchOnFillTimeout(iCancellationSignal);
            }
            remoteFillService.dispatchOnFillRequestTimeout(this);
        }

        @Override
        public void run() {
            synchronized (this.mLock) {
                if (isCancelledLocked()) {
                    if (Helper.sDebug) {
                        Slog.d(RemoteFillService.LOG_TAG, "run() called after canceled: " + this.mRequest);
                    }
                    return;
                }
                RemoteFillService service = getService();
                if (service != null) {
                    try {
                        service.mAutoFillService.onFillRequest(this.mRequest, this.mCallback);
                    } catch (RemoteException e) {
                        Slog.e(RemoteFillService.LOG_TAG, "Error calling on fill request", e);
                        service.dispatchOnFillRequestFailure(this, null);
                    }
                }
            }
        }

        @Override
        public boolean cancel() {
            ICancellationSignal iCancellationSignal;
            if (!super.cancel()) {
                return false;
            }
            synchronized (this.mLock) {
                iCancellationSignal = this.mCancellation;
            }
            if (iCancellationSignal != null) {
                try {
                    iCancellationSignal.cancel();
                    return true;
                } catch (RemoteException e) {
                    Slog.e(RemoteFillService.LOG_TAG, "Error cancelling a fill request", e);
                    return true;
                }
            }
            return true;
        }
    }

    private static final class PendingSaveRequest extends PendingRequest {
        private final ISaveCallback mCallback;
        private final SaveRequest mRequest;

        public PendingSaveRequest(SaveRequest saveRequest, RemoteFillService remoteFillService) {
            super(remoteFillService);
            this.mRequest = saveRequest;
            this.mCallback = new ISaveCallback.Stub() {
                public void onSuccess(IntentSender intentSender) {
                    RemoteFillService service;
                    if (PendingSaveRequest.this.finish() && (service = PendingSaveRequest.this.getService()) != null) {
                        service.dispatchOnSaveRequestSuccess(PendingSaveRequest.this, intentSender);
                    }
                }

                public void onFailure(CharSequence charSequence) {
                    RemoteFillService service;
                    if (PendingSaveRequest.this.finish() && (service = PendingSaveRequest.this.getService()) != null) {
                        service.dispatchOnSaveRequestFailure(PendingSaveRequest.this, charSequence);
                    }
                }
            };
        }

        @Override
        void onTimeout(RemoteFillService remoteFillService) {
            remoteFillService.dispatchOnSaveRequestFailure(this, null);
        }

        @Override
        public void run() {
            RemoteFillService service = getService();
            if (service != null) {
                try {
                    service.mAutoFillService.onSaveRequest(this.mRequest, this.mCallback);
                } catch (RemoteException e) {
                    Slog.e(RemoteFillService.LOG_TAG, "Error calling on save request", e);
                    service.dispatchOnSaveRequestFailure(this, null);
                }
            }
        }

        @Override
        public boolean isFinal() {
            return true;
        }
    }
}
