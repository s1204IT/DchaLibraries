package com.android.printservice.recommendation.plugin.hp;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Pair;
import com.android.printservice.recommendation.util.NsdResolveQueue;
import java.util.LinkedList;

final class ServiceResolveQueue {
    private final NsdManager mNsdManager;
    private static Object sLock = new Object();
    private static ServiceResolveQueue sInstance = null;
    private final LinkedList<Pair<NsdServiceInfo, ResolveCallback>> mQueue = new LinkedList<>();
    private final Object mLock = new Object();
    private Pair<NsdServiceInfo, ResolveCallback> mCurrentRequest = null;
    private final NsdResolveQueue mNsdResolveQueue = NsdResolveQueue.getInstance();

    public interface ResolveCallback {
        void serviceResolved(NsdServiceInfo nsdServiceInfo);
    }

    public static void createInstance(NsdManager nsdManager) {
        if (sInstance == null) {
            sInstance = new ServiceResolveQueue(nsdManager);
        }
    }

    public static ServiceResolveQueue getInstance(NsdManager nsdManager) {
        ServiceResolveQueue serviceResolveQueue;
        synchronized (sLock) {
            createInstance(nsdManager);
            serviceResolveQueue = sInstance;
        }
        return serviceResolveQueue;
    }

    public ServiceResolveQueue(NsdManager nsdManager) {
        this.mNsdManager = nsdManager;
    }

    public void queueRequest(NsdServiceInfo nsdServiceInfo, ResolveCallback resolveCallback) {
        synchronized (this.mLock) {
            Pair<NsdServiceInfo, ResolveCallback> pairCreate = Pair.create(nsdServiceInfo, resolveCallback);
            if (this.mQueue.contains(pairCreate)) {
                return;
            }
            this.mQueue.add(pairCreate);
            makeNextRequest();
        }
    }

    public void removeRequest(NsdServiceInfo nsdServiceInfo, ResolveCallback resolveCallback) {
        synchronized (this.mLock) {
            Pair pairCreate = Pair.create(nsdServiceInfo, resolveCallback);
            this.mQueue.remove(pairCreate);
            if (this.mCurrentRequest != null && pairCreate.equals(this.mCurrentRequest)) {
                this.mCurrentRequest = null;
            }
        }
    }

    private void makeNextRequest() {
        synchronized (this.mLock) {
            if (this.mCurrentRequest != null) {
                return;
            }
            if (this.mQueue.isEmpty()) {
                return;
            }
            this.mCurrentRequest = this.mQueue.removeFirst();
            this.mNsdResolveQueue.resolve(this.mNsdManager, (NsdServiceInfo) this.mCurrentRequest.first, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                    synchronized (ServiceResolveQueue.this.mLock) {
                        if (ServiceResolveQueue.this.mCurrentRequest != null) {
                            ServiceResolveQueue.this.mQueue.add(ServiceResolveQueue.this.mCurrentRequest);
                        }
                        ServiceResolveQueue.this.makeNextRequest();
                    }
                }

                @Override
                public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                    synchronized (ServiceResolveQueue.this.mLock) {
                        if (ServiceResolveQueue.this.mCurrentRequest != null) {
                            ((ResolveCallback) ServiceResolveQueue.this.mCurrentRequest.second).serviceResolved(nsdServiceInfo);
                            ServiceResolveQueue.this.mCurrentRequest = null;
                        }
                        ServiceResolveQueue.this.makeNextRequest();
                    }
                }
            });
        }
    }
}
