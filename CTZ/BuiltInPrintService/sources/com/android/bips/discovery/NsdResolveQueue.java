package com.android.bips.discovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import com.android.bips.discovery.NsdResolveQueue;
import java.util.LinkedList;

public class NsdResolveQueue {
    private static final String TAG = NsdResolveQueue.class.getSimpleName();
    private final Handler mMainHandler;
    private final NsdManager mNsdManager;
    private LinkedList<NsdResolveRequest> mResolveRequests = new LinkedList<>();

    public NsdResolveQueue(Context context, NsdManager nsdManager) {
        this.mNsdManager = nsdManager;
        this.mMainHandler = new Handler(context.getMainLooper());
    }

    NsdManager getNsdManager() {
        return this.mNsdManager;
    }

    public NsdResolveRequest resolve(NsdServiceInfo nsdServiceInfo, NsdManager.ResolveListener resolveListener) {
        NsdResolveRequest nsdResolveRequest = new NsdResolveRequest(this.mNsdManager, nsdServiceInfo, resolveListener);
        this.mResolveRequests.addLast(nsdResolveRequest);
        if (this.mResolveRequests.size() == 1) {
            resolveNextRequest();
        }
        return nsdResolveRequest;
    }

    private void resolveNextRequest() {
        if (this.mResolveRequests.isEmpty()) {
            return;
        }
        this.mResolveRequests.getFirst().start();
    }

    class NsdResolveRequest implements NsdManager.ResolveListener {
        private final NsdManager.ResolveListener mListener;
        private final NsdManager mNsdManager;
        private final NsdServiceInfo mServiceInfo;
        private long mStartTime;

        private NsdResolveRequest(NsdManager nsdManager, NsdServiceInfo nsdServiceInfo, NsdManager.ResolveListener resolveListener) {
            this.mNsdManager = nsdManager;
            this.mServiceInfo = nsdServiceInfo;
            this.mListener = resolveListener;
        }

        private void start() {
            this.mStartTime = System.currentTimeMillis();
            this.mNsdManager.resolveService(this.mServiceInfo, this);
        }

        void cancel() {
            if (!NsdResolveQueue.this.mResolveRequests.isEmpty() && NsdResolveQueue.this.mResolveRequests.get(0) != this) {
                NsdResolveQueue.this.mResolveRequests.remove(this);
            }
        }

        @Override
        public void onResolveFailed(final NsdServiceInfo nsdServiceInfo, final int i) {
            NsdResolveQueue.this.mMainHandler.post(new Runnable() {
                @Override
                public final void run() {
                    NsdResolveQueue.NsdResolveRequest.lambda$onResolveFailed$0(this.f$0, nsdServiceInfo, i);
                }
            });
        }

        public static void lambda$onResolveFailed$0(NsdResolveRequest nsdResolveRequest, NsdServiceInfo nsdServiceInfo, int i) {
            nsdResolveRequest.mListener.onResolveFailed(nsdServiceInfo, i);
            NsdResolveQueue.this.mResolveRequests.pop();
            NsdResolveQueue.this.resolveNextRequest();
        }

        @Override
        public void onServiceResolved(final NsdServiceInfo nsdServiceInfo) {
            NsdResolveQueue.this.mMainHandler.post(new Runnable() {
                @Override
                public final void run() {
                    NsdResolveQueue.NsdResolveRequest.lambda$onServiceResolved$1(this.f$0, nsdServiceInfo);
                }
            });
        }

        public static void lambda$onServiceResolved$1(NsdResolveRequest nsdResolveRequest, NsdServiceInfo nsdServiceInfo) {
            nsdResolveRequest.mListener.onServiceResolved(nsdServiceInfo);
            NsdResolveQueue.this.mResolveRequests.pop();
            NsdResolveQueue.this.resolveNextRequest();
        }
    }
}
