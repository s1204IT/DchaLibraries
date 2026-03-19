package android.content;

import android.accounts.Account;
import android.content.ISyncAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractThreadedSyncAdapter {

    @Deprecated
    public static final int LOG_SYNC_DETAILS = 2743;
    private boolean mAllowParallelSyncs;
    private final boolean mAutoInitialize;
    private final Context mContext;
    private final ISyncAdapterImpl mISyncAdapterImpl;
    private final AtomicInteger mNumSyncStarts;
    private final Object mSyncThreadLock;
    private final HashMap<Account, SyncThread> mSyncThreads;

    public abstract void onPerformSync(Account account, Bundle bundle, String str, ContentProviderClient contentProviderClient, SyncResult syncResult);

    public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        this.mSyncThreads = new HashMap<>();
        this.mSyncThreadLock = new Object();
        this.mContext = context;
        this.mISyncAdapterImpl = new ISyncAdapterImpl(this, null);
        this.mNumSyncStarts = new AtomicInteger(0);
        this.mAutoInitialize = autoInitialize;
        this.mAllowParallelSyncs = allowParallelSyncs;
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
        ISyncAdapterImpl(AbstractThreadedSyncAdapter this$0, ISyncAdapterImpl iSyncAdapterImpl) {
            this();
        }

        private ISyncAdapterImpl() {
        }

        @Override
        public void startSync(ISyncContext syncContext, String authority, Account account, Bundle extras) {
            boolean alreadyInProgress;
            SyncContext syncContextClient = new SyncContext(syncContext);
            Account threadsKey = AbstractThreadedSyncAdapter.this.toSyncKey(account);
            synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                if (!AbstractThreadedSyncAdapter.this.mSyncThreads.containsKey(threadsKey)) {
                    if (AbstractThreadedSyncAdapter.this.mAutoInitialize && extras != null && extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)) {
                        try {
                            if (ContentResolver.getIsSyncable(account, authority) < 0) {
                                ContentResolver.setIsSyncable(account, authority, 1);
                            }
                            return;
                        } finally {
                            syncContextClient.onFinished(new SyncResult());
                        }
                    }
                    SyncThread syncThread = new SyncThread(AbstractThreadedSyncAdapter.this, "SyncAdapterThread-" + AbstractThreadedSyncAdapter.this.mNumSyncStarts.incrementAndGet(), syncContextClient, authority, account, extras, null);
                    AbstractThreadedSyncAdapter.this.mSyncThreads.put(threadsKey, syncThread);
                    syncThread.start();
                    alreadyInProgress = false;
                } else {
                    alreadyInProgress = true;
                }
                if (!alreadyInProgress) {
                    return;
                }
                syncContextClient.onFinished(SyncResult.ALREADY_IN_PROGRESS);
            }
        }

        @Override
        public void cancelSync(ISyncContext syncContext) {
            SyncThread info = null;
            synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                Iterator current$iterator = AbstractThreadedSyncAdapter.this.mSyncThreads.values().iterator();
                while (true) {
                    if (!current$iterator.hasNext()) {
                        break;
                    }
                    SyncThread current = (SyncThread) current$iterator.next();
                    if (current.mSyncContext.getSyncContextBinder() == syncContext.asBinder()) {
                        info = current;
                        break;
                    }
                }
            }
            if (info == null) {
                return;
            }
            if (AbstractThreadedSyncAdapter.this.mAllowParallelSyncs) {
                AbstractThreadedSyncAdapter.this.onSyncCanceled(info);
            } else {
                AbstractThreadedSyncAdapter.this.onSyncCanceled();
            }
        }

        @Override
        public void initialize(Account account, String authority) throws RemoteException {
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, true);
            startSync(null, authority, account, extras);
        }
    }

    private class SyncThread extends Thread {
        private final Account mAccount;
        private final String mAuthority;
        private final Bundle mExtras;
        private final SyncContext mSyncContext;
        private final Account mThreadsKey;

        SyncThread(AbstractThreadedSyncAdapter this$0, String name, SyncContext syncContext, String authority, Account account, Bundle extras, SyncThread syncThread) {
            this(name, syncContext, authority, account, extras);
        }

        private SyncThread(String name, SyncContext syncContext, String authority, Account account, Bundle extras) {
            super(name);
            this.mSyncContext = syncContext;
            this.mAuthority = authority;
            this.mAccount = account;
            this.mExtras = extras;
            this.mThreadsKey = AbstractThreadedSyncAdapter.this.toSyncKey(account);
        }

        @Override
        public void run() {
            Object obj;
            Process.setThreadPriority(10);
            Trace.traceBegin(128L, this.mAuthority);
            SyncResult syncResult = new SyncResult();
            ContentProviderClient contentProviderClient = null;
            try {
                try {
                } catch (SecurityException e) {
                    AbstractThreadedSyncAdapter.this.onSecurityException(this.mAccount, this.mExtras, this.mAuthority, syncResult);
                    syncResult.databaseError = true;
                    Trace.traceEnd(128L);
                    if (0 != 0) {
                        contentProviderClient.release();
                    }
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    obj = AbstractThreadedSyncAdapter.this.mSyncThreadLock;
                    synchronized (obj) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                }
                if (isCanceled()) {
                    Trace.traceEnd(128L);
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    return;
                }
                ContentProviderClient provider = AbstractThreadedSyncAdapter.this.mContext.getContentResolver().acquireContentProviderClient(this.mAuthority);
                if (provider != null) {
                    AbstractThreadedSyncAdapter.this.onPerformSync(this.mAccount, this.mExtras, this.mAuthority, provider, syncResult);
                } else {
                    syncResult.databaseError = true;
                }
                Trace.traceEnd(128L);
                if (provider != null) {
                    provider.release();
                }
                if (!isCanceled()) {
                    this.mSyncContext.onFinished(syncResult);
                }
                obj = AbstractThreadedSyncAdapter.this.mSyncThreadLock;
                synchronized (obj) {
                    AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                }
            } catch (Throwable th) {
                Trace.traceEnd(128L);
                if (0 != 0) {
                    contentProviderClient.release();
                }
                if (!isCanceled()) {
                    this.mSyncContext.onFinished(syncResult);
                }
                synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                    AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    throw th;
                }
            }
        }

        private boolean isCanceled() {
            return Thread.currentThread().isInterrupted();
        }
    }

    public final IBinder getSyncAdapterBinder() {
        return this.mISyncAdapterImpl.asBinder();
    }

    public void onSecurityException(Account account, Bundle extras, String authority, SyncResult syncResult) {
    }

    public void onSyncCanceled() {
        SyncThread syncThread;
        synchronized (this.mSyncThreadLock) {
            syncThread = this.mSyncThreads.get(null);
        }
        if (syncThread == null) {
            return;
        }
        syncThread.interrupt();
    }

    public void onSyncCanceled(Thread thread) {
        thread.interrupt();
    }
}
