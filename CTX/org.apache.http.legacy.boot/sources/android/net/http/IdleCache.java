package android.net.http;

import android.os.Process;
import android.os.SystemClock;
import org.apache.http.HttpHost;

class IdleCache {
    private static final int CHECK_INTERVAL = 2000;
    private static final int EMPTY_CHECK_MAX = 5;
    private static final int IDLE_CACHE_MAX = 8;
    private static final int TIMEOUT = 6000;
    private Entry[] mEntries = new Entry[IDLE_CACHE_MAX];
    private int mCount = 0;
    private IdleReaper mThread = null;
    private int mCached = 0;
    private int mReused = 0;

    class Entry {
        Connection mConnection;
        HttpHost mHost;
        long mTimeout;

        Entry() {
        }
    }

    IdleCache() {
        for (int i = 0; i < IDLE_CACHE_MAX; i++) {
            this.mEntries[i] = new Entry();
        }
    }

    synchronized boolean cacheConnection(HttpHost httpHost, Connection connection) {
        boolean z;
        z = false;
        if (this.mCount < IDLE_CACHE_MAX) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            int i = 0;
            while (true) {
                if (i >= IDLE_CACHE_MAX) {
                    break;
                }
                Entry entry = this.mEntries[i];
                if (entry.mHost == null) {
                    break;
                }
                i++;
            }
        }
        return z;
    }

    synchronized Connection getConnection(HttpHost httpHost) {
        Connection connection;
        if (this.mCount > 0) {
            for (int i = 0; i < IDLE_CACHE_MAX; i++) {
                Entry entry = this.mEntries[i];
                HttpHost httpHost2 = entry.mHost;
                if (httpHost2 != null && httpHost2.equals(httpHost)) {
                    connection = entry.mConnection;
                    entry.mHost = null;
                    entry.mConnection = null;
                    this.mCount--;
                    break;
                }
            }
            connection = null;
        } else {
            connection = null;
        }
        return connection;
    }

    synchronized void clear() {
        for (int i = 0; this.mCount > 0 && i < IDLE_CACHE_MAX; i++) {
            Entry entry = this.mEntries[i];
            if (entry.mHost != null) {
                entry.mHost = null;
                entry.mConnection.closeConnection();
                entry.mConnection = null;
                this.mCount--;
            }
        }
    }

    private synchronized void clearIdle() {
        if (this.mCount > 0) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            for (int i = 0; i < IDLE_CACHE_MAX; i++) {
                Entry entry = this.mEntries[i];
                if (entry.mHost != null && jUptimeMillis > entry.mTimeout) {
                    entry.mHost = null;
                    entry.mConnection.closeConnection();
                    entry.mConnection = null;
                    this.mCount--;
                }
            }
        }
    }

    private class IdleReaper extends Thread {
        private IdleReaper() {
        }

        @Override
        public void run() {
            setName("IdleReaper");
            Process.setThreadPriority(10);
            synchronized (IdleCache.this) {
                while (true) {
                    for (int i = 0; i < 5; i++) {
                        try {
                            IdleCache.this.wait(2000L);
                        } catch (InterruptedException e) {
                        }
                        if (IdleCache.this.mCount == 0) {
                        }
                    }
                    IdleCache.this.mThread = null;
                    IdleCache.this.clearIdle();
                }
            }
        }
    }
}
