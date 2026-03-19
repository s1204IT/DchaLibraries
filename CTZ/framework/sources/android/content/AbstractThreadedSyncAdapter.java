package android.content;

import android.accounts.Account;
import android.content.ISyncAdapter;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public abstract class AbstractThreadedSyncAdapter {
    private static final boolean ENABLE_LOG;

    @Deprecated
    public static final int LOG_SYNC_DETAILS = 2743;
    private static final String TAG = "SyncAdapter";
    private boolean mAllowParallelSyncs;
    private final boolean mAutoInitialize;
    private final Context mContext;
    private final ISyncAdapterImpl mISyncAdapterImpl;
    private final AtomicInteger mNumSyncStarts;
    private final Object mSyncThreadLock;
    private final HashMap<Account, SyncThread> mSyncThreads;

    public abstract void onPerformSync(Account account, Bundle bundle, String str, ContentProviderClient contentProviderClient, SyncResult syncResult);

    static {
        ENABLE_LOG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 3);
    }

    public AbstractThreadedSyncAdapter(Context context, boolean z) {
        this(context, z, false);
    }

    public AbstractThreadedSyncAdapter(Context context, boolean z, boolean z2) {
        this.mSyncThreads = new HashMap<>();
        this.mSyncThreadLock = new Object();
        this.mContext = context;
        this.mISyncAdapterImpl = new ISyncAdapterImpl();
        this.mNumSyncStarts = new AtomicInteger(0);
        this.mAutoInitialize = z;
        this.mAllowParallelSyncs = z2;
    }

    public Context getContext() {
        return this.mContext;
    }

    private Account toSyncKey(Account account) {
        if (this.mAllowParallelSyncs) {
            return account;
        }
        return null;
    }

    private class ISyncAdapterImpl extends ISyncAdapter.Stub {
        private ISyncAdapterImpl() {
        }

        @Override
        public void onUnsyncableAccount(ISyncAdapterUnsyncableAccountCallback iSyncAdapterUnsyncableAccountCallback) {
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((AbstractThreadedSyncAdapter) obj).handleOnUnsyncableAccount((ISyncAdapterUnsyncableAccountCallback) obj2);
                }
            }, AbstractThreadedSyncAdapter.this, iSyncAdapterUnsyncableAccountCallback));
        }

        @Override
        public void startSync(ISyncContext iSyncContext, String str, Account account, Bundle bundle) {
            boolean z;
            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                if (bundle != null) {
                    bundle.size();
                }
                Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() start " + str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + account + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + bundle);
            }
            try {
                try {
                    SyncContext syncContext = new SyncContext(iSyncContext);
                    Account syncKey = AbstractThreadedSyncAdapter.this.toSyncKey(account);
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        boolean z2 = false;
                        if (AbstractThreadedSyncAdapter.this.mSyncThreads.containsKey(syncKey)) {
                            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                                Log.d(AbstractThreadedSyncAdapter.TAG, "  alreadyInProgress");
                            }
                            z2 = true;
                        } else {
                            if (AbstractThreadedSyncAdapter.this.mAutoInitialize && bundle != null && bundle.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)) {
                                try {
                                    if (ContentResolver.getIsSyncable(account, str) < 0) {
                                        ContentResolver.setIsSyncable(account, str, 1);
                                    }
                                    if (z) {
                                        return;
                                    } else {
                                        return;
                                    }
                                } finally {
                                    syncContext.onFinished(new SyncResult());
                                }
                            }
                            SyncThread syncThread = new SyncThread("SyncAdapterThread-" + AbstractThreadedSyncAdapter.this.mNumSyncStarts.incrementAndGet(), syncContext, str, account, bundle);
                            AbstractThreadedSyncAdapter.this.mSyncThreads.put(syncKey, syncThread);
                            syncThread.start();
                        }
                        if (z2) {
                            syncContext.onFinished(SyncResult.ALREADY_IN_PROGRESS);
                        }
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                            Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() finishing");
                        }
                    }
                } catch (Error | RuntimeException e) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() caught exception", e);
                    }
                    throw e;
                }
            } finally {
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() finishing");
                }
            }
        }

        @Override
        public void cancelSync(ISyncContext iSyncContext) {
            SyncThread syncThread = null;
            try {
                try {
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        Iterator it = AbstractThreadedSyncAdapter.this.mSyncThreads.values().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            SyncThread syncThread2 = (SyncThread) it.next();
                            if (syncThread2.mSyncContext.getSyncContextBinder() == iSyncContext.asBinder()) {
                                syncThread = syncThread2;
                                break;
                            }
                        }
                    }
                    if (syncThread != null) {
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                            Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() " + syncThread.mAuthority + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + syncThread.mAccount);
                        }
                        if (AbstractThreadedSyncAdapter.this.mAllowParallelSyncs) {
                            AbstractThreadedSyncAdapter.this.onSyncCanceled(syncThread);
                        } else {
                            AbstractThreadedSyncAdapter.this.onSyncCanceled();
                        }
                    } else if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.w(AbstractThreadedSyncAdapter.TAG, "cancelSync() unknown context");
                    }
                } catch (Error | RuntimeException e) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() caught exception", e);
                    }
                    throw e;
                }
            } finally {
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() finishing");
                }
            }
        }
    }

    private class SyncThread extends Thread {
        private final Account mAccount;
        private final String mAuthority;
        private final Bundle mExtras;
        private final SyncContext mSyncContext;
        private final Account mThreadsKey;

        private SyncThread(String str, SyncContext syncContext, String str2, Account account, Bundle bundle) {
            super(str);
            this.mSyncContext = syncContext;
            this.mAuthority = str2;
            this.mAccount = account;
            this.mExtras = bundle;
            this.mThreadsKey = AbstractThreadedSyncAdapter.this.toSyncKey(account);
        }

        @Override
        public void run() throws Throwable {
            Process.setThreadPriority(10);
            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                Log.d(AbstractThreadedSyncAdapter.TAG, "Thread started");
            }
            Trace.traceBegin(128L, this.mAuthority);
            SyncResult syncResult = new SyncResult();
            ContentProviderClient contentProviderClient = null;
            try {
                try {
                    try {
                    } catch (Error | RuntimeException e) {
                        e = e;
                    }
                } catch (SecurityException e2) {
                    e = e2;
                }
                if (isCanceled()) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "Already canceled");
                    }
                    Trace.traceEnd(128L);
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                        return;
                    }
                    return;
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "Calling onPerformSync...");
                }
                ContentProviderClient contentProviderClientAcquireContentProviderClient = AbstractThreadedSyncAdapter.this.mContext.getContentResolver().acquireContentProviderClient(this.mAuthority);
                try {
                    if (contentProviderClientAcquireContentProviderClient != null) {
                        AbstractThreadedSyncAdapter.this.onPerformSync(this.mAccount, this.mExtras, this.mAuthority, contentProviderClientAcquireContentProviderClient, syncResult);
                    } else {
                        syncResult.databaseError = true;
                    }
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "onPerformSync done");
                    }
                    Trace.traceEnd(128L);
                    if (contentProviderClientAcquireContentProviderClient != null) {
                        contentProviderClientAcquireContentProviderClient.release();
                    }
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    if (!AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        return;
                    }
                } catch (Error | RuntimeException e3) {
                    e = e3;
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "caught exception", e);
                    }
                    throw e;
                } catch (SecurityException e4) {
                    e = e4;
                    contentProviderClient = contentProviderClientAcquireContentProviderClient;
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "SecurityException", e);
                    }
                    AbstractThreadedSyncAdapter.this.onSecurityException(this.mAccount, this.mExtras, this.mAuthority, syncResult);
                    syncResult.databaseError = true;
                    Trace.traceEnd(128L);
                    if (contentProviderClient != null) {
                        contentProviderClient.release();
                    }
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    if (!AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    contentProviderClient = contentProviderClientAcquireContentProviderClient;
                    Trace.traceEnd(128L);
                    if (contentProviderClient != null) {
                        contentProviderClient.release();
                    }
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                    }
                    throw th;
                }
                Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private boolean isCanceled() {
            return Thread.currentThread().isInterrupted();
        }
    }

    public final IBinder getSyncAdapterBinder() {
        return this.mISyncAdapterImpl.asBinder();
    }

    private void handleOnUnsyncableAccount(ISyncAdapterUnsyncableAccountCallback iSyncAdapterUnsyncableAccountCallback) {
        boolean zOnUnsyncableAccount;
        try {
            zOnUnsyncableAccount = onUnsyncableAccount();
        } catch (RuntimeException e) {
            Log.e(TAG, "Exception while calling onUnsyncableAccount, assuming 'true'", e);
            zOnUnsyncableAccount = true;
        }
        try {
            iSyncAdapterUnsyncableAccountCallback.onUnsyncableAccountDone(zOnUnsyncableAccount);
        } catch (RemoteException e2) {
            Log.e(TAG, "Could not report result of onUnsyncableAccount", e2);
        }
    }

    public boolean onUnsyncableAccount() {
        return true;
    }

    public void onSecurityException(Account account, Bundle bundle, String str, SyncResult syncResult) {
    }

    public void onSyncCanceled() {
        SyncThread syncThread;
        synchronized (this.mSyncThreadLock) {
            syncThread = this.mSyncThreads.get(null);
        }
        if (syncThread != null) {
            syncThread.interrupt();
        }
    }

    public void onSyncCanceled(Thread thread) {
        thread.interrupt();
    }
}
