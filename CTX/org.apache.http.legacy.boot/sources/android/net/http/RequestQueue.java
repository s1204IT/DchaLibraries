package android.net.http;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.compatibility.WebAddress;
import android.util.Log;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.http.HttpHost;

public class RequestQueue implements RequestFeeder {
    private static final int CONNECTION_COUNT = 4;
    private final ActivePool mActivePool;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final LinkedHashMap<HttpHost, LinkedList<Request>> mPending;
    private BroadcastReceiver mProxyChangeReceiver;
    private HttpHost mProxyHost;

    interface ConnectionManager {
        Connection getConnection(Context context, HttpHost httpHost);

        HttpHost getProxyHost();

        boolean recycleConnection(Connection connection);
    }

    class ActivePool implements ConnectionManager {
        private int mConnectionCount;
        IdleCache mIdleCache = new IdleCache();
        ConnectionThread[] mThreads;
        private int mTotalConnection;
        private int mTotalRequest;

        static int access$408(ActivePool activePool) {
            int i = activePool.mTotalRequest;
            activePool.mTotalRequest = i + 1;
            return i;
        }

        ActivePool(int i) {
            this.mConnectionCount = i;
            this.mThreads = new ConnectionThread[this.mConnectionCount];
            for (int i2 = 0; i2 < this.mConnectionCount; i2++) {
                this.mThreads[i2] = new ConnectionThread(RequestQueue.this.mContext, i2, this, RequestQueue.this);
            }
        }

        void startup() {
            for (int i = 0; i < this.mConnectionCount; i++) {
                this.mThreads[i].start();
            }
        }

        void shutdown() {
            for (int i = 0; i < this.mConnectionCount; i++) {
                this.mThreads[i].requestStop();
            }
        }

        void startConnectionThread() {
            synchronized (RequestQueue.this) {
                RequestQueue.this.notify();
            }
        }

        public void startTiming() {
            for (int i = 0; i < this.mConnectionCount; i++) {
                ConnectionThread connectionThread = this.mThreads[i];
                connectionThread.mCurrentThreadTime = -1L;
                connectionThread.mTotalThreadTime = 0L;
            }
            this.mTotalRequest = 0;
            this.mTotalConnection = 0;
        }

        public void stopTiming() {
            int i = 0;
            for (int i2 = 0; i2 < this.mConnectionCount; i2++) {
                ConnectionThread connectionThread = this.mThreads[i2];
                if (connectionThread.mCurrentThreadTime != -1) {
                    i = (int) (((long) i) + connectionThread.mTotalThreadTime);
                }
                connectionThread.mCurrentThreadTime = 0L;
            }
            Log.d("Http", "Http thread used " + i + " ms  for " + this.mTotalRequest + " requests and " + this.mTotalConnection + " new connections");
        }

        void logState() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.mConnectionCount; i++) {
                sb.append(this.mThreads[i] + "\n");
            }
            HttpLog.v(sb.toString());
        }

        @Override
        public HttpHost getProxyHost() {
            return RequestQueue.this.mProxyHost;
        }

        void disablePersistence() {
            for (int i = 0; i < this.mConnectionCount; i++) {
                Connection connection = this.mThreads[i].mConnection;
                if (connection != null) {
                    connection.setCanPersist(false);
                }
            }
            this.mIdleCache.clear();
        }

        ConnectionThread getThread(HttpHost httpHost) {
            synchronized (RequestQueue.this) {
                for (int i = 0; i < this.mThreads.length; i++) {
                    ConnectionThread connectionThread = this.mThreads[i];
                    Connection connection = connectionThread.mConnection;
                    if (connection != null && connection.mHost.equals(httpHost)) {
                        return connectionThread;
                    }
                }
                return null;
            }
        }

        @Override
        public Connection getConnection(Context context, HttpHost httpHost) {
            HttpHost httpHostDetermineHost = RequestQueue.this.determineHost(httpHost);
            Connection connection = this.mIdleCache.getConnection(httpHostDetermineHost);
            if (connection == null) {
                this.mTotalConnection++;
                return Connection.getConnection(RequestQueue.this.mContext, httpHostDetermineHost, RequestQueue.this.mProxyHost, RequestQueue.this);
            }
            return connection;
        }

        @Override
        public boolean recycleConnection(Connection connection) {
            return this.mIdleCache.cacheConnection(connection.getHost(), connection);
        }
    }

    public RequestQueue(Context context) {
        this(context, 4);
    }

    public RequestQueue(Context context, int i) {
        this.mProxyHost = null;
        this.mContext = context;
        this.mPending = new LinkedHashMap<>(32);
        this.mActivePool = new ActivePool(i);
        this.mActivePool.startup();
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
    }

    public synchronized void enablePlatformNotifications() {
        if (this.mProxyChangeReceiver == null) {
            this.mProxyChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    RequestQueue.this.setProxyConfig();
                }
            };
            this.mContext.registerReceiver(this.mProxyChangeReceiver, new IntentFilter("android.intent.action.PROXY_CHANGE"));
        }
        setProxyConfig();
    }

    public synchronized void disablePlatformNotifications() {
        if (this.mProxyChangeReceiver != null) {
            this.mContext.unregisterReceiver(this.mProxyChangeReceiver);
            this.mProxyChangeReceiver = null;
        }
    }

    private synchronized void setProxyConfig() {
        String host;
        NetworkInfo activeNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if ((activeNetworkInfo != null && activeNetworkInfo.getType() == 1) || (host = Proxy.getHost(this.mContext)) == null) {
            this.mProxyHost = null;
        } else {
            this.mActivePool.disablePersistence();
            this.mProxyHost = new HttpHost(host, Proxy.getPort(this.mContext), HttpHost.DEFAULT_SCHEME_NAME);
        }
    }

    public HttpHost getProxyHost() {
        return this.mProxyHost;
    }

    public RequestHandle queueRequest(String str, String str2, Map<String, String> map, EventHandler eventHandler, InputStream inputStream, int i) {
        return queueRequest(str, new WebAddress(str), str2, map, eventHandler, inputStream, i);
    }

    public RequestHandle queueRequest(String str, WebAddress webAddress, String str2, Map<String, String> map, EventHandler eventHandler, InputStream inputStream, int i) {
        Request request = new Request(str2, new HttpHost(webAddress.getHost(), webAddress.getPort(), webAddress.getScheme()), this.mProxyHost, webAddress.getPath(), inputStream, i, eventHandler == null ? new LoggingEventHandler() : eventHandler, map);
        queueRequest(request, false);
        ActivePool.access$408(this.mActivePool);
        this.mActivePool.startConnectionThread();
        return new RequestHandle(this, str, webAddress, str2, map, inputStream, i, request);
    }

    private static class SyncFeeder implements RequestFeeder {
        private Request mRequest;

        SyncFeeder() {
        }

        @Override
        public Request getRequest() {
            Request request = this.mRequest;
            this.mRequest = null;
            return request;
        }

        @Override
        public Request getRequest(HttpHost httpHost) {
            return getRequest();
        }

        @Override
        public boolean haveRequest(HttpHost httpHost) {
            return this.mRequest != null;
        }

        @Override
        public void requeueRequest(Request request) {
            this.mRequest = request;
        }
    }

    public RequestHandle queueSynchronousRequest(String str, WebAddress webAddress, String str2, Map<String, String> map, EventHandler eventHandler, InputStream inputStream, int i) {
        HttpHost httpHost = new HttpHost(webAddress.getHost(), webAddress.getPort(), webAddress.getScheme());
        return new RequestHandle(this, str, webAddress, str2, map, inputStream, i, new Request(str2, httpHost, this.mProxyHost, webAddress.getPath(), inputStream, i, eventHandler, map), Connection.getConnection(this.mContext, determineHost(httpHost), this.mProxyHost, new SyncFeeder()));
    }

    private HttpHost determineHost(HttpHost httpHost) {
        if (this.mProxyHost == null || "https".equals(httpHost.getSchemeName())) {
            return httpHost;
        }
        return this.mProxyHost;
    }

    synchronized boolean requestsPending() {
        return !this.mPending.isEmpty();
    }

    synchronized void dump() {
        HttpLog.v("dump()");
        StringBuilder sb = new StringBuilder();
        if (!this.mPending.isEmpty()) {
            Iterator<Map.Entry<HttpHost, LinkedList<Request>>> it = this.mPending.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                Map.Entry<HttpHost, LinkedList<Request>> next = it.next();
                String hostName = next.getKey().getHostName();
                StringBuilder sb2 = new StringBuilder();
                sb2.append("p");
                int i2 = i + 1;
                sb2.append(i);
                sb2.append(" ");
                sb2.append(hostName);
                sb2.append(" ");
                StringBuilder sb3 = new StringBuilder(sb2.toString());
                next.getValue().listIterator(0);
                while (it.hasNext()) {
                    sb3.append(((Request) it.next()) + " ");
                }
                sb.append((CharSequence) sb3);
                sb.append("\n");
                i = i2;
            }
        }
        HttpLog.v(sb.toString());
    }

    @Override
    public synchronized Request getRequest() {
        Request requestRemoveFirst;
        requestRemoveFirst = null;
        if (!this.mPending.isEmpty()) {
            requestRemoveFirst = removeFirst(this.mPending);
        }
        return requestRemoveFirst;
    }

    @Override
    public synchronized Request getRequest(HttpHost httpHost) {
        Request request;
        request = null;
        if (this.mPending.containsKey(httpHost)) {
            LinkedList<Request> linkedList = this.mPending.get(httpHost);
            Request requestRemoveFirst = linkedList.removeFirst();
            if (linkedList.isEmpty()) {
                this.mPending.remove(httpHost);
            }
            request = requestRemoveFirst;
        }
        return request;
    }

    @Override
    public synchronized boolean haveRequest(HttpHost httpHost) {
        return this.mPending.containsKey(httpHost);
    }

    @Override
    public void requeueRequest(Request request) {
        queueRequest(request, true);
    }

    public void shutdown() {
        this.mActivePool.shutdown();
    }

    protected synchronized void queueRequest(Request request, boolean z) {
        LinkedList<Request> linkedList;
        HttpHost httpHost = request.mProxyHost == null ? request.mHost : request.mProxyHost;
        if (this.mPending.containsKey(httpHost)) {
            linkedList = this.mPending.get(httpHost);
        } else {
            LinkedList<Request> linkedList2 = new LinkedList<>();
            this.mPending.put(httpHost, linkedList2);
            linkedList = linkedList2;
        }
        if (z) {
            linkedList.addFirst(request);
        } else {
            linkedList.add(request);
        }
    }

    public void startTiming() {
        this.mActivePool.startTiming();
    }

    public void stopTiming() {
        this.mActivePool.stopTiming();
    }

    private Request removeFirst(LinkedHashMap<HttpHost, LinkedList<Request>> linkedHashMap) {
        Iterator<Map.Entry<HttpHost, LinkedList<Request>>> it = linkedHashMap.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<HttpHost, LinkedList<Request>> next = it.next();
            LinkedList<Request> value = next.getValue();
            Request requestRemoveFirst = value.removeFirst();
            if (!value.isEmpty()) {
                return requestRemoveFirst;
            }
            linkedHashMap.remove(next.getKey());
            return requestRemoveFirst;
        }
        return null;
    }
}
