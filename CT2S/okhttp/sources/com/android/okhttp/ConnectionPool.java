package com.android.okhttp;

import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.Util;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {
    private static final long DEFAULT_KEEP_ALIVE_DURATION_MS = 300000;
    private static final int MAX_CONNECTIONS_TO_CLEANUP = 2;
    private static final ConnectionPool systemDefault;
    private final long keepAliveDurationNs;
    private final int maxIdleConnections;
    private final LinkedList<Connection> connections = new LinkedList<>();
    private final ExecutorService executorService = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory("OkHttp ConnectionPool", true));
    private CleanMode cleanMode = CleanMode.NORMAL;
    private final Runnable drainModeRunnable = new Runnable() {
        @Override
        public void run() {
            ConnectionPool.this.connectionsCleanupRunnable.run();
            synchronized (ConnectionPool.this) {
                if (ConnectionPool.this.connections.size() > 0) {
                    try {
                        long keepAliveDurationMillis = ConnectionPool.this.keepAliveDurationNs / 1000000;
                        ConnectionPool.this.wait(keepAliveDurationMillis);
                    } catch (InterruptedException e) {
                    }
                    ConnectionPool.this.executorService.execute(this);
                } else {
                    ConnectionPool.this.cleanMode = CleanMode.DRAINED;
                }
            }
        }
    };
    private final Runnable connectionsCleanupRunnable = new Runnable() {
        @Override
        public void run() {
            List<Connection> expiredConnections = new ArrayList<>(ConnectionPool.MAX_CONNECTIONS_TO_CLEANUP);
            int idleConnectionCount = 0;
            synchronized (ConnectionPool.this) {
                ListIterator<Connection> i = ConnectionPool.this.connections.listIterator(ConnectionPool.this.connections.size());
                while (i.hasPrevious()) {
                    Connection connection = i.previous();
                    if (!connection.isAlive() || connection.isExpired(ConnectionPool.this.keepAliveDurationNs)) {
                        i.remove();
                        expiredConnections.add(connection);
                        if (expiredConnections.size() == ConnectionPool.MAX_CONNECTIONS_TO_CLEANUP) {
                            break;
                        }
                    } else if (connection.isIdle()) {
                        idleConnectionCount++;
                    }
                }
                ListIterator<Connection> i2 = ConnectionPool.this.connections.listIterator(ConnectionPool.this.connections.size());
                while (i2.hasPrevious() && idleConnectionCount > ConnectionPool.this.maxIdleConnections) {
                    Connection connection2 = i2.previous();
                    if (connection2.isIdle()) {
                        expiredConnections.add(connection2);
                        i2.remove();
                        idleConnectionCount--;
                    }
                }
            }
            for (Connection expiredConnection : expiredConnections) {
                Util.closeQuietly(expiredConnection);
            }
        }
    };

    private enum CleanMode {
        NORMAL,
        DRAINING,
        DRAINED
    }

    static {
        String keepAlive = System.getProperty("http.keepAlive");
        String keepAliveDuration = System.getProperty("http.keepAliveDuration");
        String maxIdleConnections = System.getProperty("http.maxConnections");
        long keepAliveDurationMs = keepAliveDuration != null ? Long.parseLong(keepAliveDuration) : DEFAULT_KEEP_ALIVE_DURATION_MS;
        if (keepAlive != null && !Boolean.parseBoolean(keepAlive)) {
            systemDefault = new ConnectionPool(0, keepAliveDurationMs);
        } else if (maxIdleConnections != null) {
            systemDefault = new ConnectionPool(Integer.parseInt(maxIdleConnections), keepAliveDurationMs);
        } else {
            systemDefault = new ConnectionPool(5, keepAliveDurationMs);
        }
    }

    public ConnectionPool(int maxIdleConnections, long keepAliveDurationMs) {
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationNs = keepAliveDurationMs * 1000 * 1000;
    }

    List<Connection> getConnections() {
        ArrayList arrayList;
        waitForCleanupCallableToRun();
        synchronized (this) {
            arrayList = new ArrayList(this.connections);
        }
        return arrayList;
    }

    private void waitForCleanupCallableToRun() {
        try {
            this.executorService.submit(new Runnable() {
                @Override
                public void run() {
                }
            }).get();
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    public static ConnectionPool getDefault() {
        return systemDefault;
    }

    public synchronized int getConnectionCount() {
        return this.connections.size();
    }

    public synchronized int getSpdyConnectionCount() {
        int total;
        total = 0;
        for (Connection connection : this.connections) {
            if (connection.isSpdy()) {
                total++;
            }
        }
        return total;
    }

    public synchronized int getHttpConnectionCount() {
        int total;
        total = 0;
        for (Connection connection : this.connections) {
            if (!connection.isSpdy()) {
                total++;
            }
        }
        return total;
    }

    public synchronized Connection get(Address address) {
        Connection foundConnection;
        foundConnection = null;
        ListIterator<Connection> i = this.connections.listIterator(this.connections.size());
        while (i.hasPrevious()) {
            Connection connection = i.previous();
            if (connection.getRoute().getAddress().equals(address) && connection.isAlive() && System.nanoTime() - connection.getIdleStartTimeNs() < this.keepAliveDurationNs) {
                i.remove();
                if (!connection.isSpdy()) {
                    try {
                        Platform.get().tagSocket(connection.getSocket());
                    } catch (SocketException e) {
                        Util.closeQuietly(connection);
                        Platform.get().logW("Unable to tagSocket(): " + e);
                    }
                }
                foundConnection = connection;
                break;
            }
        }
        if (foundConnection != null && foundConnection.isSpdy()) {
            this.connections.addFirst(foundConnection);
        }
        scheduleCleanupAsRequired();
        return foundConnection;
    }

    public void recycle(Connection connection) {
        if (!connection.isSpdy() && connection.clearOwner()) {
            if (!connection.isAlive()) {
                Util.closeQuietly(connection);
                return;
            }
            try {
                Platform.get().untagSocket(connection.getSocket());
                synchronized (this) {
                    this.connections.addFirst(connection);
                    connection.incrementRecycleCount();
                    connection.resetIdleStartTime();
                    scheduleCleanupAsRequired();
                }
            } catch (SocketException e) {
                Platform.get().logW("Unable to untagSocket(): " + e);
                Util.closeQuietly(connection);
            }
        }
    }

    public void share(Connection connection) {
        if (!connection.isSpdy()) {
            throw new IllegalArgumentException();
        }
        if (connection.isAlive()) {
            synchronized (this) {
                this.connections.addFirst(connection);
                scheduleCleanupAsRequired();
            }
        }
    }

    public void evictAll() {
        List<Connection> connections;
        synchronized (this) {
            connections = new ArrayList<>(this.connections);
            this.connections.clear();
        }
        int size = connections.size();
        for (int i = 0; i < size; i++) {
            Util.closeQuietly(connections.get(i));
        }
    }

    public void enterDrainMode() {
        synchronized (this) {
            this.cleanMode = CleanMode.DRAINING;
            this.executorService.execute(this.drainModeRunnable);
        }
    }

    public boolean isDrained() {
        boolean z;
        synchronized (this) {
            z = this.cleanMode == CleanMode.DRAINED;
        }
        return z;
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$com$squareup$okhttp$ConnectionPool$CleanMode = new int[CleanMode.values().length];

        static {
            try {
                $SwitchMap$com$squareup$okhttp$ConnectionPool$CleanMode[CleanMode.NORMAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$squareup$okhttp$ConnectionPool$CleanMode[CleanMode.DRAINING.ordinal()] = ConnectionPool.MAX_CONNECTIONS_TO_CLEANUP;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$squareup$okhttp$ConnectionPool$CleanMode[CleanMode.DRAINED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void scheduleCleanupAsRequired() {
        switch (AnonymousClass4.$SwitchMap$com$squareup$okhttp$ConnectionPool$CleanMode[this.cleanMode.ordinal()]) {
            case 1:
                this.executorService.execute(this.connectionsCleanupRunnable);
                break;
            case 3:
                this.cleanMode = CleanMode.DRAINING;
                this.executorService.execute(this.drainModeRunnable);
                break;
        }
    }
}
