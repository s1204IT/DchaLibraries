package com.android.server.textclassifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.textclassifier.ITextClassificationCallback;
import android.service.textclassifier.ITextClassifierService;
import android.service.textclassifier.ITextLinksCallback;
import android.service.textclassifier.ITextSelectionCallback;
import android.service.textclassifier.TextClassifierService;
import android.util.Slog;
import android.util.SparseArray;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

public final class TextClassificationManagerService extends ITextClassifierService.Stub {
    private static final String LOG_TAG = "TextClassificationManagerService";
    private final Context mContext;
    private final Object mLock;

    @GuardedBy("mLock")
    final SparseArray<UserState> mUserStates;

    public static final class Lifecycle extends SystemService {
        private final TextClassificationManagerService mManagerService;

        public Lifecycle(Context context) {
            super(context);
            this.mManagerService = new TextClassificationManagerService(context);
        }

        @Override
        public void onStart() {
            try {
                publishBinderService("textclassification", this.mManagerService);
            } catch (Throwable th) {
                Slog.e(TextClassificationManagerService.LOG_TAG, "Could not start the TextClassificationManagerService.", th);
            }
        }

        @Override
        public void onStartUser(int i) {
            processAnyPendingWork(i);
        }

        @Override
        public void onUnlockUser(int i) {
            processAnyPendingWork(i);
        }

        private void processAnyPendingWork(int i) {
            synchronized (this.mManagerService.mLock) {
                this.mManagerService.getUserStateLocked(i).bindIfHasPendingRequestsLocked();
            }
        }

        @Override
        public void onStopUser(int i) {
            synchronized (this.mManagerService.mLock) {
                UserState userStatePeekUserStateLocked = this.mManagerService.peekUserStateLocked(i);
                if (userStatePeekUserStateLocked != null) {
                    userStatePeekUserStateLocked.mConnection.cleanupService();
                    this.mManagerService.mUserStates.remove(i);
                }
            }
        }
    }

    private TextClassificationManagerService(Context context) {
        this.mUserStates = new SparseArray<>();
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mLock = new Object();
    }

    public void onSuggestSelection(final TextClassificationSessionId textClassificationSessionId, final TextSelection.Request request, final ITextSelectionCallback iTextSelectionCallback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(iTextSelectionCallback);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (!callingUserStateLocked.bindLocked()) {
                iTextSelectionCallback.onFailure();
            } else if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onSuggestSelection(textClassificationSessionId, request, iTextSelectionCallback);
            } else {
                Queue<PendingRequest> queue = callingUserStateLocked.mPendingRequests;
                FunctionalUtils.ThrowingRunnable throwingRunnable = new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onSuggestSelection(textClassificationSessionId, request, iTextSelectionCallback);
                    }
                };
                Objects.requireNonNull(iTextSelectionCallback);
                queue.add(new PendingRequest(throwingRunnable, new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() {
                        iTextSelectionCallback.onFailure();
                    }
                }, iTextSelectionCallback.asBinder(), this, callingUserStateLocked));
            }
        }
    }

    public void onClassifyText(final TextClassificationSessionId textClassificationSessionId, final TextClassification.Request request, final ITextClassificationCallback iTextClassificationCallback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(iTextClassificationCallback);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (!callingUserStateLocked.bindLocked()) {
                iTextClassificationCallback.onFailure();
            } else if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onClassifyText(textClassificationSessionId, request, iTextClassificationCallback);
            } else {
                Queue<PendingRequest> queue = callingUserStateLocked.mPendingRequests;
                FunctionalUtils.ThrowingRunnable throwingRunnable = new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onClassifyText(textClassificationSessionId, request, iTextClassificationCallback);
                    }
                };
                Objects.requireNonNull(iTextClassificationCallback);
                queue.add(new PendingRequest(throwingRunnable, new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() {
                        iTextClassificationCallback.onFailure();
                    }
                }, iTextClassificationCallback.asBinder(), this, callingUserStateLocked));
            }
        }
    }

    public void onGenerateLinks(final TextClassificationSessionId textClassificationSessionId, final TextLinks.Request request, final ITextLinksCallback iTextLinksCallback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(iTextLinksCallback);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (!callingUserStateLocked.bindLocked()) {
                iTextLinksCallback.onFailure();
            } else if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onGenerateLinks(textClassificationSessionId, request, iTextLinksCallback);
            } else {
                Queue<PendingRequest> queue = callingUserStateLocked.mPendingRequests;
                FunctionalUtils.ThrowingRunnable throwingRunnable = new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onGenerateLinks(textClassificationSessionId, request, iTextLinksCallback);
                    }
                };
                Objects.requireNonNull(iTextLinksCallback);
                queue.add(new PendingRequest(throwingRunnable, new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() {
                        iTextLinksCallback.onFailure();
                    }
                }, iTextLinksCallback.asBinder(), this, callingUserStateLocked));
            }
        }
    }

    public void onSelectionEvent(final TextClassificationSessionId textClassificationSessionId, final SelectionEvent selectionEvent) throws RemoteException {
        Preconditions.checkNotNull(selectionEvent);
        validateInput(selectionEvent.getPackageName(), this.mContext);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onSelectionEvent(textClassificationSessionId, selectionEvent);
            } else {
                callingUserStateLocked.mPendingRequests.add(new PendingRequest(new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onSelectionEvent(textClassificationSessionId, selectionEvent);
                    }
                }, null, null, this, callingUserStateLocked));
            }
        }
    }

    public void onCreateTextClassificationSession(final TextClassificationContext textClassificationContext, final TextClassificationSessionId textClassificationSessionId) throws RemoteException {
        Preconditions.checkNotNull(textClassificationSessionId);
        Preconditions.checkNotNull(textClassificationContext);
        validateInput(textClassificationContext.getPackageName(), this.mContext);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onCreateTextClassificationSession(textClassificationContext, textClassificationSessionId);
            } else {
                callingUserStateLocked.mPendingRequests.add(new PendingRequest(new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onCreateTextClassificationSession(textClassificationContext, textClassificationSessionId);
                    }
                }, null, null, this, callingUserStateLocked));
            }
        }
    }

    public void onDestroyTextClassificationSession(final TextClassificationSessionId textClassificationSessionId) throws RemoteException {
        Preconditions.checkNotNull(textClassificationSessionId);
        synchronized (this.mLock) {
            UserState callingUserStateLocked = getCallingUserStateLocked();
            if (callingUserStateLocked.isBoundLocked()) {
                callingUserStateLocked.mService.onDestroyTextClassificationSession(textClassificationSessionId);
            } else {
                callingUserStateLocked.mPendingRequests.add(new PendingRequest(new FunctionalUtils.ThrowingRunnable() {
                    public final void runOrThrow() throws RemoteException {
                        this.f$0.onDestroyTextClassificationSession(textClassificationSessionId);
                    }
                }, null, null, this, callingUserStateLocked));
            }
        }
    }

    private UserState getCallingUserStateLocked() {
        return getUserStateLocked(UserHandle.getCallingUserId());
    }

    private UserState getUserStateLocked(int i) {
        UserState userState = this.mUserStates.get(i);
        if (userState == null) {
            UserState userState2 = new UserState(i, this.mContext, this.mLock);
            this.mUserStates.put(i, userState2);
            return userState2;
        }
        return userState;
    }

    UserState peekUserStateLocked(int i) {
        return this.mUserStates.get(i);
    }

    private static final class PendingRequest implements IBinder.DeathRecipient {
        private final IBinder mBinder;
        private final Runnable mOnServiceFailure;

        @GuardedBy("mLock")
        private final UserState mOwningUser;
        private final Runnable mRequest;
        private final TextClassificationManagerService mService;

        PendingRequest(FunctionalUtils.ThrowingRunnable throwingRunnable, FunctionalUtils.ThrowingRunnable throwingRunnable2, IBinder iBinder, TextClassificationManagerService textClassificationManagerService, UserState userState) {
            this.mRequest = TextClassificationManagerService.logOnFailure((FunctionalUtils.ThrowingRunnable) Preconditions.checkNotNull(throwingRunnable), "handling pending request");
            this.mOnServiceFailure = TextClassificationManagerService.logOnFailure(throwingRunnable2, "notifying callback of service failure");
            this.mBinder = iBinder;
            this.mService = textClassificationManagerService;
            this.mOwningUser = userState;
            if (this.mBinder != null) {
                try {
                    this.mBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void binderDied() {
            synchronized (this.mService.mLock) {
                removeLocked();
            }
        }

        @GuardedBy("mLock")
        private void removeLocked() {
            this.mOwningUser.mPendingRequests.remove(this);
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }
    }

    private static Runnable logOnFailure(FunctionalUtils.ThrowingRunnable throwingRunnable, final String str) {
        if (throwingRunnable == null) {
            return null;
        }
        return FunctionalUtils.handleExceptions(throwingRunnable, new Consumer() {
            @Override
            public final void accept(Object obj) {
                Slog.d(TextClassificationManagerService.LOG_TAG, "Error " + str + ": " + ((Throwable) obj).getMessage());
            }
        });
    }

    private static void validateInput(String str, Context context) throws RemoteException {
        try {
            Preconditions.checkArgument(Binder.getCallingUid() == context.getPackageManager().getPackageUid(str, 0));
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException | NullPointerException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private static final class UserState {

        @GuardedBy("mLock")
        boolean mBinding;
        final TextClassifierServiceConnection mConnection;
        private final Context mContext;
        private final Object mLock;

        @GuardedBy("mLock")
        final Queue<PendingRequest> mPendingRequests;

        @GuardedBy("mLock")
        ITextClassifierService mService;
        final int mUserId;

        private UserState(int i, Context context, Object obj) {
            this.mConnection = new TextClassifierServiceConnection();
            this.mPendingRequests = new ArrayDeque();
            this.mUserId = i;
            this.mContext = (Context) Preconditions.checkNotNull(context);
            this.mLock = Preconditions.checkNotNull(obj);
        }

        @GuardedBy("mLock")
        boolean isBoundLocked() {
            return this.mService != null;
        }

        @GuardedBy("mLock")
        private void handlePendingRequestsLocked() {
            while (true) {
                PendingRequest pendingRequestPoll = this.mPendingRequests.poll();
                if (pendingRequestPoll != null) {
                    if (isBoundLocked()) {
                        pendingRequestPoll.mRequest.run();
                    } else if (pendingRequestPoll.mOnServiceFailure != null) {
                        pendingRequestPoll.mOnServiceFailure.run();
                    }
                    if (pendingRequestPoll.mBinder != null) {
                        pendingRequestPoll.mBinder.unlinkToDeath(pendingRequestPoll, 0);
                    }
                } else {
                    return;
                }
            }
        }

        private boolean bindIfHasPendingRequestsLocked() {
            return !this.mPendingRequests.isEmpty() && bindLocked();
        }

        private boolean bindLocked() {
            if (isBoundLocked() || this.mBinding) {
                return true;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ComponentName serviceComponentName = TextClassifierService.getServiceComponentName(this.mContext);
                if (serviceComponentName != null) {
                    Intent component = new Intent("android.service.textclassifier.TextClassifierService").setComponent(serviceComponentName);
                    Slog.d(TextClassificationManagerService.LOG_TAG, "Binding to " + component.getComponent());
                    boolean zBindServiceAsUser = this.mContext.bindServiceAsUser(component, this.mConnection, 67108865, UserHandle.of(this.mUserId));
                    this.mBinding = zBindServiceAsUser;
                    return zBindServiceAsUser;
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private final class TextClassifierServiceConnection implements ServiceConnection {
            private TextClassifierServiceConnection() {
            }

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                init(ITextClassifierService.Stub.asInterface(iBinder));
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                cleanupService();
            }

            @Override
            public void onBindingDied(ComponentName componentName) {
                cleanupService();
            }

            @Override
            public void onNullBinding(ComponentName componentName) {
                cleanupService();
            }

            void cleanupService() {
                init(null);
            }

            private void init(ITextClassifierService iTextClassifierService) {
                synchronized (UserState.this.mLock) {
                    UserState.this.mService = iTextClassifierService;
                    UserState.this.mBinding = false;
                    UserState.this.handlePendingRequestsLocked();
                }
            }
        }
    }
}
