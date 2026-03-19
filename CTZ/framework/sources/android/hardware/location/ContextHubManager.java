package android.hardware.location;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.hardware.location.ContextHubTransaction;
import android.hardware.location.IContextHubCallback;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.IContextHubService;
import android.hardware.location.IContextHubTransactionCallback;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.concurrent.Executor;

@SystemApi
public final class ContextHubManager {
    private static final String TAG = "ContextHubManager";
    private Callback mCallback;
    private Handler mCallbackHandler;

    @Deprecated
    private ICallback mLocalCallback;
    private final Looper mMainLooper;
    private final IContextHubCallback.Stub mClientCallback = new IContextHubCallback.Stub() {
        @Override
        public void onMessageReceipt(final int i, final int i2, final ContextHubMessage contextHubMessage) {
            Handler handler;
            if (ContextHubManager.this.mCallback == null) {
                if (ContextHubManager.this.mLocalCallback != null) {
                    synchronized (this) {
                        ContextHubManager.this.mLocalCallback.onMessageReceipt(i, i2, contextHubMessage);
                    }
                    return;
                }
                return;
            }
            synchronized (this) {
                final Callback callback = ContextHubManager.this.mCallback;
                if (ContextHubManager.this.mCallbackHandler != null) {
                    handler = ContextHubManager.this.mCallbackHandler;
                } else {
                    handler = new Handler(ContextHubManager.this.mMainLooper);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onMessageReceipt(i, i2, contextHubMessage);
                    }
                });
            }
        }
    };
    private final IContextHubService mService = IContextHubService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.CONTEXTHUB_SERVICE));

    @Deprecated
    public interface ICallback {
        void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);
    }

    @Deprecated
    public static abstract class Callback {
        public abstract void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);

        protected Callback() {
        }
    }

    @Deprecated
    public int[] getContextHubHandles() {
        try {
            return this.mService.getContextHubHandles();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public ContextHubInfo getContextHubInfo(int i) {
        try {
            return this.mService.getContextHubInfo(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int loadNanoApp(int i, NanoApp nanoApp) {
        try {
            return this.mService.loadNanoApp(i, nanoApp);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int unloadNanoApp(int i) {
        try {
            return this.mService.unloadNanoApp(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NanoAppInstanceInfo getNanoAppInstanceInfo(int i) {
        try {
            return this.mService.getNanoAppInstanceInfo(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int[] findNanoAppOnHub(int i, NanoAppFilter nanoAppFilter) {
        try {
            return this.mService.findNanoAppOnHub(i, nanoAppFilter);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int sendMessage(int i, int i2, ContextHubMessage contextHubMessage) {
        try {
            return this.mService.sendMessage(i, i2, contextHubMessage);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ContextHubInfo> getContextHubs() {
        try {
            return this.mService.getContextHubs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private IContextHubTransactionCallback createTransactionCallback(final ContextHubTransaction<Void> contextHubTransaction) {
        return new IContextHubTransactionCallback.Stub() {
            @Override
            public void onQueryResponse(int i, List<NanoAppState> list) {
                Log.e(ContextHubManager.TAG, "Received a query callback on a non-query request");
                contextHubTransaction.setResponse(new ContextHubTransaction.Response(7, null));
            }

            @Override
            public void onTransactionComplete(int i) {
                contextHubTransaction.setResponse(new ContextHubTransaction.Response(i, null));
            }
        };
    }

    private IContextHubTransactionCallback createQueryCallback(final ContextHubTransaction<List<NanoAppState>> contextHubTransaction) {
        return new IContextHubTransactionCallback.Stub() {
            @Override
            public void onQueryResponse(int i, List<NanoAppState> list) {
                contextHubTransaction.setResponse(new ContextHubTransaction.Response(i, list));
            }

            @Override
            public void onTransactionComplete(int i) {
                Log.e(ContextHubManager.TAG, "Received a non-query callback on a query request");
                contextHubTransaction.setResponse(new ContextHubTransaction.Response(7, null));
            }
        };
    }

    public ContextHubTransaction<Void> loadNanoApp(ContextHubInfo contextHubInfo, NanoAppBinary nanoAppBinary) {
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        Preconditions.checkNotNull(nanoAppBinary, "NanoAppBinary cannot be null");
        ContextHubTransaction<Void> contextHubTransaction = new ContextHubTransaction<>(0);
        try {
            this.mService.loadNanoAppOnHub(contextHubInfo.getId(), createTransactionCallback(contextHubTransaction), nanoAppBinary);
            return contextHubTransaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> unloadNanoApp(ContextHubInfo contextHubInfo, long j) {
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> contextHubTransaction = new ContextHubTransaction<>(1);
        try {
            this.mService.unloadNanoAppFromHub(contextHubInfo.getId(), createTransactionCallback(contextHubTransaction), j);
            return contextHubTransaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> enableNanoApp(ContextHubInfo contextHubInfo, long j) {
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> contextHubTransaction = new ContextHubTransaction<>(2);
        try {
            this.mService.enableNanoApp(contextHubInfo.getId(), createTransactionCallback(contextHubTransaction), j);
            return contextHubTransaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> disableNanoApp(ContextHubInfo contextHubInfo, long j) {
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> contextHubTransaction = new ContextHubTransaction<>(3);
        try {
            this.mService.disableNanoApp(contextHubInfo.getId(), createTransactionCallback(contextHubTransaction), j);
            return contextHubTransaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<List<NanoAppState>> queryNanoApps(ContextHubInfo contextHubInfo) {
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<List<NanoAppState>> contextHubTransaction = new ContextHubTransaction<>(4);
        try {
            this.mService.queryNanoApps(contextHubInfo.getId(), createQueryCallback(contextHubTransaction));
            return contextHubTransaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int registerCallback(Callback callback) {
        return registerCallback(callback, null);
    }

    @Deprecated
    public int registerCallback(ICallback iCallback) {
        if (this.mLocalCallback != null) {
            Log.w(TAG, "Max number of local callbacks reached!");
            return -1;
        }
        this.mLocalCallback = iCallback;
        return 0;
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int registerCallback(Callback callback, Handler handler) {
        synchronized (this) {
            if (this.mCallback != null) {
                Log.w(TAG, "Max number of callbacks reached!");
                return -1;
            }
            this.mCallback = callback;
            this.mCallbackHandler = handler;
            return 0;
        }
    }

    class AnonymousClass3 extends IContextHubClientCallback.Stub {
        final ContextHubClientCallback val$callback;
        final ContextHubClient val$client;
        final Executor val$executor;

        AnonymousClass3(Executor executor, ContextHubClientCallback contextHubClientCallback, ContextHubClient contextHubClient) {
            this.val$executor = executor;
            this.val$callback = contextHubClientCallback;
            this.val$client = contextHubClient;
        }

        @Override
        public void onMessageFromNanoApp(final NanoAppMessage nanoAppMessage) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onMessageFromNanoApp(contextHubClient, nanoAppMessage);
                }
            });
        }

        @Override
        public void onHubReset() {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onHubReset(contextHubClient);
                }
            });
        }

        @Override
        public void onNanoAppAborted(final long j, final int i) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onNanoAppAborted(contextHubClient, j, i);
                }
            });
        }

        @Override
        public void onNanoAppLoaded(final long j) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onNanoAppLoaded(contextHubClient, j);
                }
            });
        }

        @Override
        public void onNanoAppUnloaded(final long j) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onNanoAppUnloaded(contextHubClient, j);
                }
            });
        }

        @Override
        public void onNanoAppEnabled(final long j) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onNanoAppEnabled(contextHubClient, j);
                }
            });
        }

        @Override
        public void onNanoAppDisabled(final long j) {
            Executor executor = this.val$executor;
            final ContextHubClientCallback contextHubClientCallback = this.val$callback;
            final ContextHubClient contextHubClient = this.val$client;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    contextHubClientCallback.onNanoAppDisabled(contextHubClient, j);
                }
            });
        }
    }

    private IContextHubClientCallback createClientCallback(ContextHubClient contextHubClient, ContextHubClientCallback contextHubClientCallback, Executor executor) {
        return new AnonymousClass3(executor, contextHubClientCallback, contextHubClient);
    }

    public ContextHubClient createClient(ContextHubInfo contextHubInfo, ContextHubClientCallback contextHubClientCallback, Executor executor) {
        Preconditions.checkNotNull(contextHubClientCallback, "Callback cannot be null");
        Preconditions.checkNotNull(contextHubInfo, "ContextHubInfo cannot be null");
        Preconditions.checkNotNull(executor, "Executor cannot be null");
        ContextHubClient contextHubClient = new ContextHubClient(contextHubInfo);
        try {
            contextHubClient.setClientProxy(this.mService.createClient(createClientCallback(contextHubClient, contextHubClientCallback, executor), contextHubInfo.getId()));
            return contextHubClient;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubClient createClient(ContextHubInfo contextHubInfo, ContextHubClientCallback contextHubClientCallback) {
        return createClient(contextHubInfo, contextHubClientCallback, new HandlerExecutor(Handler.getMain()));
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int unregisterCallback(Callback callback) {
        synchronized (this) {
            if (callback != this.mCallback) {
                Log.w(TAG, "Cannot recognize callback!");
                return -1;
            }
            this.mCallback = null;
            this.mCallbackHandler = null;
            return 0;
        }
    }

    @Deprecated
    public synchronized int unregisterCallback(ICallback iCallback) {
        if (iCallback != this.mLocalCallback) {
            Log.w(TAG, "Cannot recognize local callback!");
            return -1;
        }
        this.mLocalCallback = null;
        return 0;
    }

    public ContextHubManager(Context context, Looper looper) throws ServiceManager.ServiceNotFoundException {
        this.mMainLooper = looper;
        try {
            this.mService.registerCallback(this.mClientCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
