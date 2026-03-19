package com.android.printservice.recommendation.util;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import java.util.LinkedList;

public class NsdResolveQueue {
    private static NsdResolveQueue sInstance;
    private static final Object sLock = new Object();
    private final Object mLock = new Object();
    private final LinkedList<NsdResolveRequest> mResolveRequests = new LinkedList<>();

    public static NsdResolveQueue getInstance() {
        NsdResolveQueue nsdResolveQueue;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new NsdResolveQueue();
            }
            nsdResolveQueue = sInstance;
        }
        return nsdResolveQueue;
    }

    private static class NsdResolveRequest {
        final NsdManager.ResolveListener listener;
        final NsdManager nsdManager;
        final NsdServiceInfo serviceInfo;

        private NsdResolveRequest(NsdManager nsdManager, NsdServiceInfo nsdServiceInfo, NsdManager.ResolveListener resolveListener) {
            this.nsdManager = nsdManager;
            this.serviceInfo = nsdServiceInfo;
            this.listener = resolveListener;
        }
    }

    public void resolve(NsdManager nsdManager, NsdServiceInfo nsdServiceInfo, NsdManager.ResolveListener resolveListener) {
        synchronized (this.mLock) {
            this.mResolveRequests.addLast(new NsdResolveRequest(nsdManager, nsdServiceInfo, new ListenerWrapper(resolveListener)));
            if (this.mResolveRequests.size() == 1) {
                resolveNextRequest();
            }
        }
    }

    private class ListenerWrapper implements NsdManager.ResolveListener {
        private final NsdManager.ResolveListener mListener;

        private ListenerWrapper(NsdManager.ResolveListener resolveListener) {
            this.mListener = resolveListener;
        }

        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
            this.mListener.onResolveFailed(nsdServiceInfo, i);
            synchronized (NsdResolveQueue.this.mLock) {
                NsdResolveQueue.this.mResolveRequests.pop();
                NsdResolveQueue.this.resolveNextRequest();
            }
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
            this.mListener.onServiceResolved(nsdServiceInfo);
            synchronized (NsdResolveQueue.this.mLock) {
                NsdResolveQueue.this.mResolveRequests.pop();
                NsdResolveQueue.this.resolveNextRequest();
            }
        }
    }

    private void resolveNextRequest() {
        if (!this.mResolveRequests.isEmpty()) {
            NsdResolveRequest first = this.mResolveRequests.getFirst();
            first.nsdManager.resolveService(first.serviceInfo, first.listener);
        }
    }
}
