package com.android.printservice.recommendation.plugin.xerox;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import com.android.printservice.recommendation.util.DiscoveryListenerMultiplexer;
import com.android.printservice.recommendation.util.NsdResolveQueue;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class ServiceResolver {
    private final NsdManager mNSDManager;
    private final Observer mObserver;
    private final String[] mPDLs;
    private final String[] mServiceType;
    private final VendorInfo mVendorInfo;
    private final PrinterHashMap mPrinterHashMap = new PrinterHashMap();
    private final List<NsdManager.DiscoveryListener> mListeners = new ArrayList();
    private final LinkedList<NsdServiceInfo> mQueue = new LinkedList<>();
    private final Object mLock = new Object();
    private NsdServiceInfo mCurrentRequest = null;
    private final NsdResolveQueue mNsdResolveQueue = NsdResolveQueue.getInstance();

    public interface Observer {
        void dataSetChanged();
    }

    public ServiceResolver(Context context, Observer observer, VendorInfo vendorInfo, String[] strArr, String[] strArr2) {
        this.mObserver = observer;
        this.mServiceType = strArr;
        this.mNSDManager = (NsdManager) context.getSystemService("servicediscovery");
        this.mVendorInfo = vendorInfo;
        this.mPDLs = strArr2;
    }

    public void start() {
        stop();
        for (String str : this.mServiceType) {
            NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onStartDiscoveryFailed(String str2, int i) {
                }

                @Override
                public void onStopDiscoveryFailed(String str2, int i) {
                }

                @Override
                public void onDiscoveryStarted(String str2) {
                }

                @Override
                public void onDiscoveryStopped(String str2) {
                }

                @Override
                public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                    ServiceResolver.this.queueRequest(nsdServiceInfo);
                }

                @Override
                public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                    ServiceResolver.this.removeRequest(nsdServiceInfo);
                    ServiceResolver.this.printerRemoved(nsdServiceInfo);
                }
            };
            DiscoveryListenerMultiplexer.addListener(this.mNSDManager, str, discoveryListener);
            this.mListeners.add(discoveryListener);
        }
    }

    public void stop() {
        Iterator<NsdManager.DiscoveryListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            DiscoveryListenerMultiplexer.removeListener(this.mNSDManager, it.next());
        }
        this.mListeners.clear();
        clearRequests();
    }

    private void queueRequest(NsdServiceInfo nsdServiceInfo) {
        synchronized (this.mLock) {
            if (this.mQueue.contains(nsdServiceInfo)) {
                return;
            }
            this.mQueue.add(nsdServiceInfo);
            makeNextRequest();
        }
    }

    private void removeRequest(NsdServiceInfo nsdServiceInfo) {
        synchronized (this.mLock) {
            this.mQueue.remove(nsdServiceInfo);
            if (this.mCurrentRequest != null && nsdServiceInfo.equals(this.mCurrentRequest)) {
                this.mCurrentRequest = null;
            }
        }
    }

    private void clearRequests() {
        synchronized (this.mLock) {
            this.mQueue.clear();
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
            this.mNsdResolveQueue.resolve(this.mNSDManager, this.mCurrentRequest, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                    synchronized (ServiceResolver.this.mLock) {
                        if (ServiceResolver.this.mCurrentRequest != null) {
                            ServiceResolver.this.mQueue.add(ServiceResolver.this.mCurrentRequest);
                        }
                        ServiceResolver.this.makeNextRequest();
                    }
                }

                @Override
                public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                    synchronized (ServiceResolver.this.mLock) {
                        if (ServiceResolver.this.mCurrentRequest != null) {
                            ServiceResolver.this.printerFound(nsdServiceInfo);
                            ServiceResolver.this.mCurrentRequest = null;
                        }
                        ServiceResolver.this.makeNextRequest();
                    }
                }
            });
        }
    }

    private void printerFound(NsdServiceInfo nsdServiceInfo) {
        if (nsdServiceInfo == null || TextUtils.isEmpty(PrinterHashMap.getKey(nsdServiceInfo))) {
            return;
        }
        String vendor = MDnsUtils.getVendor(nsdServiceInfo);
        if (vendor == null) {
            vendor = "";
        }
        String[] strArr = this.mVendorInfo.mDNSValues;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (!vendor.equalsIgnoreCase(strArr[i])) {
                i++;
            } else {
                vendor = this.mVendorInfo.mVendorID;
                break;
            }
        }
        if (vendor != this.mVendorInfo.mVendorID && MDnsUtils.isVendorPrinter(nsdServiceInfo, this.mVendorInfo.mDNSValues)) {
            vendor = this.mVendorInfo.mVendorID;
        }
        if (vendor == this.mVendorInfo.mVendorID && MDnsUtils.checkPDLSupport(nsdServiceInfo, this.mPDLs) && this.mPrinterHashMap.addPrinter(nsdServiceInfo) == null) {
            this.mObserver.dataSetChanged();
        }
    }

    private void printerRemoved(NsdServiceInfo nsdServiceInfo) {
        if (this.mPrinterHashMap.removePrinter(nsdServiceInfo) != null) {
            this.mObserver.dataSetChanged();
        }
    }

    public ArrayList<InetAddress> getPrinters() {
        ArrayList<InetAddress> arrayList = new ArrayList<>();
        Iterator<NsdServiceInfo> it = this.mPrinterHashMap.values().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getHost());
        }
        return arrayList;
    }
}
