package com.android.bips.ipp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import com.android.bips.BuiltInPrintService;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.p2p.P2pUtils;
import com.android.bips.util.BroadcastMonitor;
import com.android.bips.util.WifiMonitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class CapabilitiesCache extends LruCache<Uri, LocalPrinterCapabilities> implements AutoCloseable {
    private static final String TAG = CapabilitiesCache.class.getSimpleName();
    private final Backend mBackend;
    private boolean mIsStopped;
    private final int mMaxConcurrent;
    private final BroadcastMonitor mP2pMonitor;
    private final Map<Uri, Request> mRequests;
    private final BuiltInPrintService mService;
    private final Set<Uri> mToEvict;
    private final Set<Uri> mToEvictP2p;
    private final WifiMonitor mWifiMonitor;

    public interface OnLocalPrinterCapabilities {
        void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities);
    }

    public CapabilitiesCache(BuiltInPrintService builtInPrintService, Backend backend, int i) {
        super(100);
        this.mRequests = new HashMap();
        this.mToEvict = new HashSet();
        this.mToEvictP2p = new HashSet();
        this.mIsStopped = false;
        this.mService = builtInPrintService;
        this.mBackend = backend;
        this.mMaxConcurrent = i;
        this.mP2pMonitor = this.mService.receiveBroadcasts(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!((NetworkInfo) intent.getParcelableExtra("networkInfo")).isConnected()) {
                    Iterator it = CapabilitiesCache.this.mToEvictP2p.iterator();
                    while (it.hasNext()) {
                        CapabilitiesCache.this.remove((Uri) it.next());
                    }
                    CapabilitiesCache.this.mToEvictP2p.clear();
                }
            }
        }, "android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mWifiMonitor = new WifiMonitor(builtInPrintService, new WifiMonitor.Listener() {
            @Override
            public final void onConnectionStateChanged(boolean z) {
                CapabilitiesCache.lambda$new$0(this.f$0, z);
            }
        });
    }

    public static void lambda$new$0(CapabilitiesCache capabilitiesCache, boolean z) {
        if (!z) {
            Iterator<Uri> it = capabilitiesCache.mToEvict.iterator();
            while (it.hasNext()) {
                capabilitiesCache.remove(it.next());
            }
            capabilitiesCache.mToEvict.clear();
        }
    }

    @Override
    public void close() {
        this.mIsStopped = true;
        this.mWifiMonitor.close();
        this.mP2pMonitor.close();
    }

    public void request(final DiscoveredPrinter discoveredPrinter, final boolean z, OnLocalPrinterCapabilities onLocalPrinterCapabilities) {
        LocalPrinterCapabilities localPrinterCapabilities = get(discoveredPrinter);
        if (localPrinterCapabilities != null && localPrinterCapabilities.nativeData != null) {
            onLocalPrinterCapabilities.onCapabilities(localPrinterCapabilities);
            return;
        }
        if (P2pUtils.isOnConnectedInterface(this.mService, discoveredPrinter)) {
            this.mToEvictP2p.add(discoveredPrinter.path);
        } else {
            this.mToEvict.add(discoveredPrinter.path);
        }
        Request requestComputeIfAbsent = this.mRequests.computeIfAbsent(discoveredPrinter.path, new Function() {
            @Override
            public final Object apply(Object obj) {
                return CapabilitiesCache.lambda$request$1(this.f$0, discoveredPrinter, z, (Uri) obj);
            }
        });
        if (z) {
            requestComputeIfAbsent.mHighPriority = true;
        }
        requestComputeIfAbsent.mCallbacks.add(onLocalPrinterCapabilities);
        startNextRequest();
    }

    public static Request lambda$request$1(CapabilitiesCache capabilitiesCache, DiscoveredPrinter discoveredPrinter, boolean z, Uri uri) {
        return capabilitiesCache.new Request(discoveredPrinter, z ? 8000L : 500L);
    }

    public LocalPrinterCapabilities get(DiscoveredPrinter discoveredPrinter) {
        return get(discoveredPrinter.path);
    }

    public void cancel(OnLocalPrinterCapabilities onLocalPrinterCapabilities) {
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<Uri, Request> entry : this.mRequests.entrySet()) {
            Request value = entry.getValue();
            value.mCallbacks.remove(onLocalPrinterCapabilities);
            if (value.mCallbacks.isEmpty()) {
                arrayList.add(entry.getKey());
                value.cancel();
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.mRequests.remove((Uri) it.next());
        }
    }

    private void startNextRequest() {
        Request nextRequest = getNextRequest();
        if (nextRequest == null) {
            return;
        }
        nextRequest.start();
    }

    private Request getNextRequest() {
        int i = 0;
        Request request = null;
        for (Request request2 : this.mRequests.values()) {
            if (request2.mQuery != null) {
                i++;
            } else if (request == null || ((!request.mHighPriority && request2.mHighPriority) || (request.mHighPriority == request2.mHighPriority && request2.mTimeout < request.mTimeout))) {
                request = request2;
            }
        }
        if (i >= this.mMaxConcurrent) {
            return null;
        }
        return request;
    }

    public class Request implements Consumer<LocalPrinterCapabilities> {
        final List<OnLocalPrinterCapabilities> mCallbacks = new ArrayList();
        boolean mHighPriority = false;
        final DiscoveredPrinter mPrinter;
        GetCapabilitiesTask mQuery;
        long mTimeout;

        Request(DiscoveredPrinter discoveredPrinter, long j) {
            this.mPrinter = discoveredPrinter;
            this.mTimeout = j;
        }

        private void start() {
            this.mQuery = CapabilitiesCache.this.mBackend.getCapabilities(this.mPrinter.path, this.mTimeout, this.mHighPriority, this);
        }

        private void cancel() {
            if (this.mQuery != null) {
                this.mQuery.forceCancel();
                this.mQuery = null;
            }
        }

        @Override
        public void accept(LocalPrinterCapabilities localPrinterCapabilities) {
            Uri uri;
            DiscoveredPrinter discoveredPrinter = this.mPrinter;
            if (!CapabilitiesCache.this.mIsStopped) {
                CapabilitiesCache.this.mRequests.remove(discoveredPrinter.path);
                if (localPrinterCapabilities != null) {
                    if (!TextUtils.isEmpty(localPrinterCapabilities.uuid)) {
                        uri = Uri.parse(localPrinterCapabilities.uuid);
                    } else {
                        uri = null;
                    }
                    if (discoveredPrinter.uuid != null && !discoveredPrinter.uuid.equals(uri)) {
                        Log.w(CapabilitiesCache.TAG, "UUID mismatch for " + discoveredPrinter + "; rejecting capabilities");
                        localPrinterCapabilities = null;
                    }
                }
                if (localPrinterCapabilities != null) {
                    CapabilitiesCache.this.put(discoveredPrinter.path, localPrinterCapabilities);
                } else {
                    if (this.mTimeout == 500) {
                        this.mTimeout = 8000L;
                        this.mQuery = null;
                        CapabilitiesCache.this.mRequests.put(discoveredPrinter.path, this);
                        CapabilitiesCache.this.startNextRequest();
                        return;
                    }
                    CapabilitiesCache.this.remove(discoveredPrinter.getUri());
                }
                Iterator<OnLocalPrinterCapabilities> it = this.mCallbacks.iterator();
                while (it.hasNext()) {
                    it.next().onCapabilities(localPrinterCapabilities);
                }
                CapabilitiesCache.this.startNextRequest();
            }
        }
    }
}
