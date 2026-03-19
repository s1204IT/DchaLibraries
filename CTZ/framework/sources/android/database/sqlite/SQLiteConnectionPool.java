package android.database.sqlite;

import android.database.sqlite.SQLiteDebug;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OperationCanceledException;
import android.os.SystemClock;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.Log;
import android.util.PrefixPrinter;
import android.util.Printer;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class SQLiteConnectionPool implements Closeable {
    static final boolean $assertionsDisabled = false;
    public static final int CONNECTION_FLAG_INTERACTIVE = 4;
    public static final int CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY = 2;
    public static final int CONNECTION_FLAG_READ_ONLY = 1;
    private static final long CONNECTION_POOL_BUSY_MILLIS = 30000;
    private static final String TAG = "SQLiteConnectionPool";
    private SQLiteConnection mAvailablePrimaryConnection;
    private final SQLiteDatabaseConfiguration mConfiguration;
    private ConnectionWaiter mConnectionWaiterPool;
    private ConnectionWaiter mConnectionWaiterQueue;

    @GuardedBy("mLock")
    private IdleConnectionHandler mIdleConnectionHandler;
    private boolean mIsOpen;
    private int mMaxConnectionPoolSize;
    private int mNextConnectionId;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final Object mLock = new Object();
    private final AtomicBoolean mConnectionLeaked = new AtomicBoolean();
    private final ArrayList<SQLiteConnection> mAvailableNonPrimaryConnections = new ArrayList<>();
    private final AtomicLong mTotalExecutionTimeCounter = new AtomicLong(0);
    private final WeakHashMap<SQLiteConnection, AcquiredConnectionStatus> mAcquiredConnections = new WeakHashMap<>();

    enum AcquiredConnectionStatus {
        NORMAL,
        RECONFIGURE,
        DISCARD
    }

    private SQLiteConnectionPool(SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration) {
        this.mConfiguration = new SQLiteDatabaseConfiguration(sQLiteDatabaseConfiguration);
        setMaxConnectionPoolSizeLocked();
        if (this.mConfiguration.idleConnectionTimeoutMs != Long.MAX_VALUE) {
            setupIdleConnectionHandler(Looper.getMainLooper(), this.mConfiguration.idleConnectionTimeoutMs);
        }
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static SQLiteConnectionPool open(SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration) {
        if (sQLiteDatabaseConfiguration == null) {
            throw new IllegalArgumentException("configuration must not be null.");
        }
        SQLiteConnectionPool sQLiteConnectionPool = new SQLiteConnectionPool(sQLiteDatabaseConfiguration);
        sQLiteConnectionPool.open();
        return sQLiteConnectionPool;
    }

    private void open() {
        this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionReleased(this.mAvailablePrimaryConnection);
            }
        }
        this.mIsOpen = true;
        this.mCloseGuard.open("close");
    }

    @Override
    public void close() {
        dispose(false);
    }

    private void dispose(boolean z) {
        if (this.mCloseGuard != null) {
            if (z) {
                this.mCloseGuard.warnIfOpen();
            }
            this.mCloseGuard.close();
        }
        if (!z) {
            synchronized (this.mLock) {
                throwIfClosedLocked();
                this.mIsOpen = false;
                closeAvailableConnectionsAndLogExceptionsLocked();
                int size = this.mAcquiredConnections.size();
                if (size != 0) {
                    Log.i(TAG, "The connection pool for " + this.mConfiguration.label + " has been closed but there are still " + size + " connections in use.  They will be closed as they are released back to the pool.");
                }
                wakeConnectionWaitersLocked();
            }
        }
    }

    public void reconfigure(SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration) {
        if (sQLiteDatabaseConfiguration == null) {
            throw new IllegalArgumentException("configuration must not be null.");
        }
        synchronized (this.mLock) {
            throwIfClosedLocked();
            boolean z = ((sQLiteDatabaseConfiguration.openFlags ^ this.mConfiguration.openFlags) & 536870912) != 0;
            if (z) {
                if (!this.mAcquiredConnections.isEmpty()) {
                    throw new IllegalStateException("Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
                }
                closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
            }
            if ((sQLiteDatabaseConfiguration.foreignKeyConstraintsEnabled != this.mConfiguration.foreignKeyConstraintsEnabled) && !this.mAcquiredConnections.isEmpty()) {
                throw new IllegalStateException("Foreign Key Constraints cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
            }
            if (!((this.mConfiguration.openFlags ^ sQLiteDatabaseConfiguration.openFlags) == 1073741824) && this.mConfiguration.openFlags != sQLiteDatabaseConfiguration.openFlags) {
                if (z) {
                    closeAvailableConnectionsAndLogExceptionsLocked();
                }
                SQLiteConnection sQLiteConnectionOpenConnectionLocked = openConnectionLocked(sQLiteDatabaseConfiguration, true);
                closeAvailableConnectionsAndLogExceptionsLocked();
                discardAcquiredConnectionsLocked();
                this.mAvailablePrimaryConnection = sQLiteConnectionOpenConnectionLocked;
                this.mConfiguration.updateParametersFrom(sQLiteDatabaseConfiguration);
                setMaxConnectionPoolSizeLocked();
            } else {
                this.mConfiguration.updateParametersFrom(sQLiteDatabaseConfiguration);
                setMaxConnectionPoolSizeLocked();
                closeExcessConnectionsAndLogExceptionsLocked();
                reconfigureAllConnectionsLocked();
            }
            wakeConnectionWaitersLocked();
        }
    }

    public SQLiteConnection acquireConnection(String str, int i, CancellationSignal cancellationSignal) {
        SQLiteConnection sQLiteConnectionWaitForConnection = waitForConnection(str, i, cancellationSignal);
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionAcquired(sQLiteConnectionWaitForConnection);
            }
        }
        return sQLiteConnectionWaitForConnection;
    }

    public void releaseConnection(SQLiteConnection sQLiteConnection) {
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionReleased(sQLiteConnection);
            }
            AcquiredConnectionStatus acquiredConnectionStatusRemove = this.mAcquiredConnections.remove(sQLiteConnection);
            if (acquiredConnectionStatusRemove == null) {
                throw new IllegalStateException("Cannot perform this operation because the specified connection was not acquired from this pool or has already been released.");
            }
            if (!this.mIsOpen) {
                closeConnectionAndLogExceptionsLocked(sQLiteConnection);
            } else if (sQLiteConnection.isPrimaryConnection()) {
                if (recycleConnectionLocked(sQLiteConnection, acquiredConnectionStatusRemove)) {
                    this.mAvailablePrimaryConnection = sQLiteConnection;
                }
                wakeConnectionWaitersLocked();
            } else if (this.mAvailableNonPrimaryConnections.size() >= this.mMaxConnectionPoolSize - 1) {
                closeConnectionAndLogExceptionsLocked(sQLiteConnection);
            } else {
                if (recycleConnectionLocked(sQLiteConnection, acquiredConnectionStatusRemove)) {
                    this.mAvailableNonPrimaryConnections.add(sQLiteConnection);
                }
                wakeConnectionWaitersLocked();
            }
        }
    }

    @GuardedBy("mLock")
    private boolean recycleConnectionLocked(SQLiteConnection sQLiteConnection, AcquiredConnectionStatus acquiredConnectionStatus) {
        if (acquiredConnectionStatus == AcquiredConnectionStatus.RECONFIGURE) {
            try {
                sQLiteConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to reconfigure released connection, closing it: " + sQLiteConnection, e);
                acquiredConnectionStatus = AcquiredConnectionStatus.DISCARD;
            }
        }
        if (acquiredConnectionStatus == AcquiredConnectionStatus.DISCARD) {
            closeConnectionAndLogExceptionsLocked(sQLiteConnection);
            return false;
        }
        return true;
    }

    public boolean shouldYieldConnection(SQLiteConnection sQLiteConnection, int i) {
        synchronized (this.mLock) {
            if (!this.mAcquiredConnections.containsKey(sQLiteConnection)) {
                throw new IllegalStateException("Cannot perform this operation because the specified connection was not acquired from this pool or has already been released.");
            }
            if (!this.mIsOpen) {
                return false;
            }
            return isSessionBlockingImportantConnectionWaitersLocked(sQLiteConnection.isPrimaryConnection(), i);
        }
    }

    public void collectDbStats(ArrayList<SQLiteDebug.DbStats> arrayList) {
        synchronized (this.mLock) {
            if (this.mAvailablePrimaryConnection != null) {
                this.mAvailablePrimaryConnection.collectDbStats(arrayList);
            }
            Iterator<SQLiteConnection> it = this.mAvailableNonPrimaryConnections.iterator();
            while (it.hasNext()) {
                it.next().collectDbStats(arrayList);
            }
            Iterator<SQLiteConnection> it2 = this.mAcquiredConnections.keySet().iterator();
            while (it2.hasNext()) {
                it2.next().collectDbStatsUnsafe(arrayList);
            }
        }
    }

    private SQLiteConnection openConnectionLocked(SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration, boolean z) {
        int i = this.mNextConnectionId;
        this.mNextConnectionId = i + 1;
        return SQLiteConnection.open(this, sQLiteDatabaseConfiguration, i, z);
    }

    void onConnectionLeaked() {
        Log.w(TAG, "A SQLiteConnection object for database '" + this.mConfiguration.label + "' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.");
        this.mConnectionLeaked.set(true);
    }

    void onStatementExecuted(long j) {
        this.mTotalExecutionTimeCounter.addAndGet(j);
    }

    @GuardedBy("mLock")
    private void closeAvailableConnectionsAndLogExceptionsLocked() {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
        if (this.mAvailablePrimaryConnection != null) {
            closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
            this.mAvailablePrimaryConnection = null;
        }
    }

    @GuardedBy("mLock")
    private boolean closeAvailableConnectionLocked(int i) {
        for (int size = this.mAvailableNonPrimaryConnections.size() - 1; size >= 0; size--) {
            SQLiteConnection sQLiteConnection = this.mAvailableNonPrimaryConnections.get(size);
            if (sQLiteConnection.getConnectionId() == i) {
                closeConnectionAndLogExceptionsLocked(sQLiteConnection);
                this.mAvailableNonPrimaryConnections.remove(size);
                return true;
            }
        }
        if (this.mAvailablePrimaryConnection != null && this.mAvailablePrimaryConnection.getConnectionId() == i) {
            closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
            this.mAvailablePrimaryConnection = null;
            return true;
        }
        return false;
    }

    @GuardedBy("mLock")
    private void closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked() {
        int size = this.mAvailableNonPrimaryConnections.size();
        for (int i = 0; i < size; i++) {
            closeConnectionAndLogExceptionsLocked(this.mAvailableNonPrimaryConnections.get(i));
        }
        this.mAvailableNonPrimaryConnections.clear();
    }

    void closeAvailableNonPrimaryConnectionsAndLogExceptions() {
        synchronized (this.mLock) {
            closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
        }
    }

    @GuardedBy("mLock")
    private void closeExcessConnectionsAndLogExceptionsLocked() {
        int size = this.mAvailableNonPrimaryConnections.size();
        while (true) {
            int i = size - 1;
            if (size > this.mMaxConnectionPoolSize - 1) {
                closeConnectionAndLogExceptionsLocked(this.mAvailableNonPrimaryConnections.remove(i));
                size = i;
            } else {
                return;
            }
        }
    }

    @GuardedBy("mLock")
    private void closeConnectionAndLogExceptionsLocked(SQLiteConnection sQLiteConnection) {
        try {
            sQLiteConnection.close();
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionClosed(sQLiteConnection);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to close connection, its fate is now in the hands of the merciful GC: " + sQLiteConnection, e);
        }
    }

    private void discardAcquiredConnectionsLocked() {
        markAcquiredConnectionsLocked(AcquiredConnectionStatus.DISCARD);
    }

    @GuardedBy("mLock")
    private void reconfigureAllConnectionsLocked() {
        if (this.mAvailablePrimaryConnection != null) {
            try {
                this.mAvailablePrimaryConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to reconfigure available primary connection, closing it: " + this.mAvailablePrimaryConnection, e);
                closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
                this.mAvailablePrimaryConnection = null;
            }
        }
        int size = this.mAvailableNonPrimaryConnections.size();
        int i = 0;
        while (i < size) {
            SQLiteConnection sQLiteConnection = this.mAvailableNonPrimaryConnections.get(i);
            try {
                sQLiteConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException e2) {
                Log.e(TAG, "Failed to reconfigure available non-primary connection, closing it: " + sQLiteConnection, e2);
                closeConnectionAndLogExceptionsLocked(sQLiteConnection);
                this.mAvailableNonPrimaryConnections.remove(i);
                size += -1;
                i--;
            }
            i++;
        }
        markAcquiredConnectionsLocked(AcquiredConnectionStatus.RECONFIGURE);
    }

    private void markAcquiredConnectionsLocked(AcquiredConnectionStatus acquiredConnectionStatus) {
        if (!this.mAcquiredConnections.isEmpty()) {
            ArrayList arrayList = new ArrayList(this.mAcquiredConnections.size());
            for (Map.Entry<SQLiteConnection, AcquiredConnectionStatus> entry : this.mAcquiredConnections.entrySet()) {
                AcquiredConnectionStatus value = entry.getValue();
                if (acquiredConnectionStatus != value && value != AcquiredConnectionStatus.DISCARD) {
                    arrayList.add(entry.getKey());
                }
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                this.mAcquiredConnections.put((SQLiteConnection) arrayList.get(i), acquiredConnectionStatus);
            }
        }
    }

    private SQLiteConnection waitForConnection(String str, int i, CancellationSignal cancellationSignal) {
        SQLiteConnection sQLiteConnectionTryAcquirePrimaryConnectionLocked;
        SQLiteConnection sQLiteConnection;
        RuntimeException runtimeException;
        long j;
        boolean z = (i & 2) != 0;
        synchronized (this.mLock) {
            throwIfClosedLocked();
            if (cancellationSignal != null) {
                cancellationSignal.throwIfCanceled();
            }
            if (!z) {
                sQLiteConnectionTryAcquirePrimaryConnectionLocked = tryAcquireNonPrimaryConnectionLocked(str, i);
            } else {
                sQLiteConnectionTryAcquirePrimaryConnectionLocked = null;
            }
            if (sQLiteConnectionTryAcquirePrimaryConnectionLocked == null) {
                sQLiteConnectionTryAcquirePrimaryConnectionLocked = tryAcquirePrimaryConnectionLocked(i);
            }
            if (sQLiteConnectionTryAcquirePrimaryConnectionLocked != null) {
                return sQLiteConnectionTryAcquirePrimaryConnectionLocked;
            }
            int priority = getPriority(i);
            final ConnectionWaiter connectionWaiterObtainConnectionWaiterLocked = obtainConnectionWaiterLocked(Thread.currentThread(), SystemClock.uptimeMillis(), priority, z, str, i);
            ConnectionWaiter connectionWaiter = this.mConnectionWaiterQueue;
            ConnectionWaiter connectionWaiter2 = null;
            while (true) {
                if (connectionWaiter == null) {
                    break;
                }
                if (priority > connectionWaiter.mPriority) {
                    connectionWaiterObtainConnectionWaiterLocked.mNext = connectionWaiter;
                    break;
                }
                connectionWaiter2 = connectionWaiter;
                connectionWaiter = connectionWaiter.mNext;
            }
            if (connectionWaiter2 != null) {
                connectionWaiter2.mNext = connectionWaiterObtainConnectionWaiterLocked;
            } else {
                this.mConnectionWaiterQueue = connectionWaiterObtainConnectionWaiterLocked;
            }
            final int i2 = connectionWaiterObtainConnectionWaiterLocked.mNonce;
            if (cancellationSignal != null) {
                cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        synchronized (SQLiteConnectionPool.this.mLock) {
                            if (connectionWaiterObtainConnectionWaiterLocked.mNonce == i2) {
                                SQLiteConnectionPool.this.cancelConnectionWaiterLocked(connectionWaiterObtainConnectionWaiterLocked);
                            }
                        }
                    }
                });
            }
            try {
                long j2 = connectionWaiterObtainConnectionWaiterLocked.mStartTime + 30000;
                long j3 = 30000;
                while (true) {
                    if (this.mConnectionLeaked.compareAndSet(true, false)) {
                        synchronized (this.mLock) {
                            wakeConnectionWaitersLocked();
                        }
                    }
                    LockSupport.parkNanos(this, j3 * TimeUtils.NANOS_PER_MS);
                    Thread.interrupted();
                    synchronized (this.mLock) {
                        throwIfClosedLocked();
                        sQLiteConnection = connectionWaiterObtainConnectionWaiterLocked.mAssignedConnection;
                        runtimeException = connectionWaiterObtainConnectionWaiterLocked.mException;
                        if (sQLiteConnection != null || runtimeException != null) {
                            break;
                        }
                        long jUptimeMillis = SystemClock.uptimeMillis();
                        if (jUptimeMillis < j2) {
                            j = jUptimeMillis - j2;
                        } else {
                            logConnectionPoolBusyLocked(jUptimeMillis - connectionWaiterObtainConnectionWaiterLocked.mStartTime, i);
                            j2 = jUptimeMillis + 30000;
                            j = 30000;
                        }
                    }
                    return sQLiteConnection;
                    j3 = j;
                }
                recycleConnectionWaiterLocked(connectionWaiterObtainConnectionWaiterLocked);
                if (sQLiteConnection != null) {
                    return sQLiteConnection;
                }
                throw runtimeException;
            } finally {
                if (cancellationSignal != null) {
                    cancellationSignal.setOnCancelListener(null);
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelConnectionWaiterLocked(ConnectionWaiter connectionWaiter) {
        ConnectionWaiter connectionWaiter2;
        if (connectionWaiter.mAssignedConnection != null || connectionWaiter.mException != null) {
            return;
        }
        ConnectionWaiter connectionWaiter3 = null;
        ConnectionWaiter connectionWaiter4 = this.mConnectionWaiterQueue;
        while (true) {
            ConnectionWaiter connectionWaiter5 = connectionWaiter4;
            connectionWaiter2 = connectionWaiter3;
            connectionWaiter3 = connectionWaiter5;
            if (connectionWaiter3 == connectionWaiter) {
                break;
            } else {
                connectionWaiter4 = connectionWaiter3.mNext;
            }
        }
        if (connectionWaiter2 != null) {
            connectionWaiter2.mNext = connectionWaiter.mNext;
        } else {
            this.mConnectionWaiterQueue = connectionWaiter.mNext;
        }
        connectionWaiter.mException = new OperationCanceledException();
        LockSupport.unpark(connectionWaiter.mThread);
        wakeConnectionWaitersLocked();
    }

    private void logConnectionPoolBusyLocked(long j, int i) {
        int i2;
        Thread threadCurrentThread = Thread.currentThread();
        StringBuilder sb = new StringBuilder();
        sb.append("The connection pool for database '");
        sb.append(this.mConfiguration.label);
        sb.append("' has been unable to grant a connection to thread ");
        sb.append(threadCurrentThread.getId());
        sb.append(" (");
        sb.append(threadCurrentThread.getName());
        sb.append(") ");
        sb.append("with flags 0x");
        sb.append(Integer.toHexString(i));
        sb.append(" for ");
        sb.append(j * 0.001f);
        sb.append(" seconds.\n");
        ArrayList<String> arrayList = new ArrayList();
        int i3 = 0;
        if (!this.mAcquiredConnections.isEmpty()) {
            Iterator<SQLiteConnection> it = this.mAcquiredConnections.keySet().iterator();
            i2 = 0;
            while (it.hasNext()) {
                String strDescribeCurrentOperationUnsafe = it.next().describeCurrentOperationUnsafe();
                if (strDescribeCurrentOperationUnsafe != null) {
                    arrayList.add(strDescribeCurrentOperationUnsafe);
                    i3++;
                } else {
                    i2++;
                }
            }
        } else {
            i2 = 0;
        }
        int size = this.mAvailableNonPrimaryConnections.size();
        if (this.mAvailablePrimaryConnection != null) {
            size++;
        }
        sb.append("Connections: ");
        sb.append(i3);
        sb.append(" active, ");
        sb.append(i2);
        sb.append(" idle, ");
        sb.append(size);
        sb.append(" available.\n");
        if (!arrayList.isEmpty()) {
            sb.append("\nRequests in progress:\n");
            for (String str : arrayList) {
                sb.append("  ");
                sb.append(str);
                sb.append("\n");
            }
        }
        Log.w(TAG, sb.toString());
    }

    @GuardedBy("mLock")
    private void wakeConnectionWaitersLocked() {
        SQLiteConnection sQLiteConnectionTryAcquirePrimaryConnectionLocked;
        ConnectionWaiter connectionWaiter = this.mConnectionWaiterQueue;
        boolean z = false;
        boolean z2 = false;
        ConnectionWaiter connectionWaiter2 = null;
        while (connectionWaiter != null) {
            boolean z3 = true;
            if (this.mIsOpen) {
                try {
                    if (connectionWaiter.mWantPrimaryConnection || z) {
                        sQLiteConnectionTryAcquirePrimaryConnectionLocked = null;
                    } else {
                        sQLiteConnectionTryAcquirePrimaryConnectionLocked = tryAcquireNonPrimaryConnectionLocked(connectionWaiter.mSql, connectionWaiter.mConnectionFlags);
                        if (sQLiteConnectionTryAcquirePrimaryConnectionLocked == null) {
                            z = true;
                        }
                    }
                    if (sQLiteConnectionTryAcquirePrimaryConnectionLocked == null && !z2 && (sQLiteConnectionTryAcquirePrimaryConnectionLocked = tryAcquirePrimaryConnectionLocked(connectionWaiter.mConnectionFlags)) == null) {
                        z2 = true;
                    }
                    if (sQLiteConnectionTryAcquirePrimaryConnectionLocked != null) {
                        connectionWaiter.mAssignedConnection = sQLiteConnectionTryAcquirePrimaryConnectionLocked;
                    } else if (!z || !z2) {
                        z3 = false;
                    } else {
                        return;
                    }
                } catch (RuntimeException e) {
                    connectionWaiter.mException = e;
                }
            }
            ConnectionWaiter connectionWaiter3 = connectionWaiter.mNext;
            if (z3) {
                if (connectionWaiter2 != null) {
                    connectionWaiter2.mNext = connectionWaiter3;
                } else {
                    this.mConnectionWaiterQueue = connectionWaiter3;
                }
                connectionWaiter.mNext = null;
                LockSupport.unpark(connectionWaiter.mThread);
            } else {
                connectionWaiter2 = connectionWaiter;
            }
            connectionWaiter = connectionWaiter3;
        }
    }

    @GuardedBy("mLock")
    private SQLiteConnection tryAcquirePrimaryConnectionLocked(int i) {
        SQLiteConnection sQLiteConnection = this.mAvailablePrimaryConnection;
        if (sQLiteConnection != null) {
            this.mAvailablePrimaryConnection = null;
            finishAcquireConnectionLocked(sQLiteConnection, i);
            return sQLiteConnection;
        }
        Iterator<SQLiteConnection> it = this.mAcquiredConnections.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().isPrimaryConnection()) {
                return null;
            }
        }
        SQLiteConnection sQLiteConnectionOpenConnectionLocked = openConnectionLocked(this.mConfiguration, true);
        finishAcquireConnectionLocked(sQLiteConnectionOpenConnectionLocked, i);
        return sQLiteConnectionOpenConnectionLocked;
    }

    @GuardedBy("mLock")
    private SQLiteConnection tryAcquireNonPrimaryConnectionLocked(String str, int i) {
        int size = this.mAvailableNonPrimaryConnections.size();
        if (size > 1 && str != null) {
            for (int i2 = 0; i2 < size; i2++) {
                SQLiteConnection sQLiteConnection = this.mAvailableNonPrimaryConnections.get(i2);
                if (sQLiteConnection.isPreparedStatementInCache(str)) {
                    this.mAvailableNonPrimaryConnections.remove(i2);
                    finishAcquireConnectionLocked(sQLiteConnection, i);
                    return sQLiteConnection;
                }
            }
        }
        if (size > 0) {
            SQLiteConnection sQLiteConnectionRemove = this.mAvailableNonPrimaryConnections.remove(size - 1);
            finishAcquireConnectionLocked(sQLiteConnectionRemove, i);
            return sQLiteConnectionRemove;
        }
        int size2 = this.mAcquiredConnections.size();
        if (this.mAvailablePrimaryConnection != null) {
            size2++;
        }
        if (size2 < this.mMaxConnectionPoolSize) {
            SQLiteConnection sQLiteConnectionOpenConnectionLocked = openConnectionLocked(this.mConfiguration, false);
            finishAcquireConnectionLocked(sQLiteConnectionOpenConnectionLocked, i);
            return sQLiteConnectionOpenConnectionLocked;
        }
        return null;
    }

    @GuardedBy("mLock")
    private void finishAcquireConnectionLocked(SQLiteConnection sQLiteConnection, int i) {
        boolean z;
        if ((i & 1) == 0) {
            z = false;
        } else {
            z = true;
        }
        try {
            sQLiteConnection.setOnlyAllowReadOnlyOperations(z);
            this.mAcquiredConnections.put(sQLiteConnection, AcquiredConnectionStatus.NORMAL);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to prepare acquired connection for session, closing it: " + sQLiteConnection + ", connectionFlags=" + i);
            closeConnectionAndLogExceptionsLocked(sQLiteConnection);
            throw e;
        }
    }

    private boolean isSessionBlockingImportantConnectionWaitersLocked(boolean z, int i) {
        ConnectionWaiter connectionWaiter = this.mConnectionWaiterQueue;
        if (connectionWaiter != null) {
            int priority = getPriority(i);
            while (priority <= connectionWaiter.mPriority) {
                if (z || !connectionWaiter.mWantPrimaryConnection) {
                    return true;
                }
                connectionWaiter = connectionWaiter.mNext;
                if (connectionWaiter == null) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    private static int getPriority(int i) {
        return (i & 4) != 0 ? 1 : 0;
    }

    private void setMaxConnectionPoolSizeLocked() {
        if (!this.mConfiguration.isInMemoryDb() && (this.mConfiguration.openFlags & 536870912) != 0) {
            this.mMaxConnectionPoolSize = SQLiteGlobal.getWALConnectionPoolSize();
        } else {
            this.mMaxConnectionPoolSize = 1;
        }
    }

    @VisibleForTesting
    public void setupIdleConnectionHandler(Looper looper, long j) {
        synchronized (this.mLock) {
            this.mIdleConnectionHandler = new IdleConnectionHandler(looper, j);
        }
    }

    void disableIdleConnectionHandler() {
        synchronized (this.mLock) {
            this.mIdleConnectionHandler = null;
        }
    }

    private void throwIfClosedLocked() {
        if (!this.mIsOpen) {
            throw new IllegalStateException("Cannot perform this operation because the connection pool has been closed.");
        }
    }

    private ConnectionWaiter obtainConnectionWaiterLocked(Thread thread, long j, int i, boolean z, String str, int i2) {
        ConnectionWaiter connectionWaiter = this.mConnectionWaiterPool;
        if (connectionWaiter != null) {
            this.mConnectionWaiterPool = connectionWaiter.mNext;
            connectionWaiter.mNext = null;
        } else {
            connectionWaiter = new ConnectionWaiter();
        }
        connectionWaiter.mThread = thread;
        connectionWaiter.mStartTime = j;
        connectionWaiter.mPriority = i;
        connectionWaiter.mWantPrimaryConnection = z;
        connectionWaiter.mSql = str;
        connectionWaiter.mConnectionFlags = i2;
        return connectionWaiter;
    }

    private void recycleConnectionWaiterLocked(ConnectionWaiter connectionWaiter) {
        connectionWaiter.mNext = this.mConnectionWaiterPool;
        connectionWaiter.mThread = null;
        connectionWaiter.mSql = null;
        connectionWaiter.mAssignedConnection = null;
        connectionWaiter.mException = null;
        connectionWaiter.mNonce++;
        this.mConnectionWaiterPool = connectionWaiter;
    }

    public void dump(Printer printer, boolean z) {
        Printer printerCreate = PrefixPrinter.create(printer, "    ");
        synchronized (this.mLock) {
            printer.println("Connection pool for " + this.mConfiguration.path + SettingsStringUtil.DELIMITER);
            StringBuilder sb = new StringBuilder();
            sb.append("  Open: ");
            sb.append(this.mIsOpen);
            printer.println(sb.toString());
            printer.println("  Max connections: " + this.mMaxConnectionPoolSize);
            printer.println("  Total execution time: " + this.mTotalExecutionTimeCounter);
            printer.println("  Configuration: openFlags=" + this.mConfiguration.openFlags + ", useCompatibilityWal=" + this.mConfiguration.useCompatibilityWal() + ", journalMode=" + TextUtils.emptyIfNull(this.mConfiguration.journalMode) + ", syncMode=" + TextUtils.emptyIfNull(this.mConfiguration.syncMode));
            if (SQLiteCompatibilityWalFlags.areFlagsSet()) {
                printer.println("  Compatibility WAL settings: compatibility_wal_supported=" + SQLiteCompatibilityWalFlags.isCompatibilityWalSupported() + ", wal_syncmode=" + SQLiteCompatibilityWalFlags.getWALSyncMode());
            }
            if (this.mConfiguration.isLookasideConfigSet()) {
                printer.println("  Lookaside config: sz=" + this.mConfiguration.lookasideSlotSize + " cnt=" + this.mConfiguration.lookasideSlotCount);
            }
            if (this.mConfiguration.idleConnectionTimeoutMs != Long.MAX_VALUE) {
                printer.println("  Idle connection timeout: " + this.mConfiguration.idleConnectionTimeoutMs);
            }
            printer.println("  Available primary connection:");
            if (this.mAvailablePrimaryConnection != null) {
                this.mAvailablePrimaryConnection.dump(printerCreate, z);
            } else {
                printerCreate.println("<none>");
            }
            printer.println("  Available non-primary connections:");
            int i = 0;
            if (!this.mAvailableNonPrimaryConnections.isEmpty()) {
                int size = this.mAvailableNonPrimaryConnections.size();
                for (int i2 = 0; i2 < size; i2++) {
                    this.mAvailableNonPrimaryConnections.get(i2).dump(printerCreate, z);
                }
            } else {
                printerCreate.println("<none>");
            }
            printer.println("  Acquired connections:");
            if (!this.mAcquiredConnections.isEmpty()) {
                for (Map.Entry<SQLiteConnection, AcquiredConnectionStatus> entry : this.mAcquiredConnections.entrySet()) {
                    entry.getKey().dumpUnsafe(printerCreate, z);
                    printerCreate.println("  Status: " + entry.getValue());
                }
            } else {
                printerCreate.println("<none>");
            }
            printer.println("  Connection waiters:");
            if (this.mConnectionWaiterQueue != null) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                ConnectionWaiter connectionWaiter = this.mConnectionWaiterQueue;
                while (connectionWaiter != null) {
                    printerCreate.println(i + ": waited for " + ((jUptimeMillis - connectionWaiter.mStartTime) * 0.001f) + " ms - thread=" + connectionWaiter.mThread + ", priority=" + connectionWaiter.mPriority + ", sql='" + connectionWaiter.mSql + "'");
                    connectionWaiter = connectionWaiter.mNext;
                    i++;
                }
            } else {
                printerCreate.println("<none>");
            }
        }
    }

    public String toString() {
        return "SQLiteConnectionPool: " + this.mConfiguration.path;
    }

    private static final class ConnectionWaiter {
        public SQLiteConnection mAssignedConnection;
        public int mConnectionFlags;
        public RuntimeException mException;
        public ConnectionWaiter mNext;
        public int mNonce;
        public int mPriority;
        public String mSql;
        public long mStartTime;
        public Thread mThread;
        public boolean mWantPrimaryConnection;

        private ConnectionWaiter() {
        }
    }

    private class IdleConnectionHandler extends Handler {
        private final long mTimeout;

        IdleConnectionHandler(Looper looper, long j) {
            super(looper);
            this.mTimeout = j;
        }

        @Override
        public void handleMessage(Message message) {
            synchronized (SQLiteConnectionPool.this.mLock) {
                if (this != SQLiteConnectionPool.this.mIdleConnectionHandler) {
                    return;
                }
                if (SQLiteConnectionPool.this.closeAvailableConnectionLocked(message.what) && Log.isLoggable(SQLiteConnectionPool.TAG, 3)) {
                    Log.d(SQLiteConnectionPool.TAG, "Closed idle connection " + SQLiteConnectionPool.this.mConfiguration.label + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + message.what + " after " + this.mTimeout);
                }
            }
        }

        void connectionReleased(SQLiteConnection sQLiteConnection) {
            sendEmptyMessageDelayed(sQLiteConnection.getConnectionId(), this.mTimeout);
        }

        void connectionAcquired(SQLiteConnection sQLiteConnection) {
            removeMessages(sQLiteConnection.getConnectionId());
        }

        void connectionClosed(SQLiteConnection sQLiteConnection) {
            removeMessages(sQLiteConnection.getConnectionId());
        }
    }
}
