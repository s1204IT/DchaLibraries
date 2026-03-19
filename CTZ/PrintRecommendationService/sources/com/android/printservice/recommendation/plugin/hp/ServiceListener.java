package com.android.printservice.recommendation.plugin.hp;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import com.android.printservice.recommendation.R;
import com.android.printservice.recommendation.plugin.hp.ServiceResolveQueue;
import com.android.printservice.recommendation.util.DiscoveryListenerMultiplexer;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServiceListener implements ServiceResolveQueue.ResolveCallback {
    private final NsdManager mNSDManager;
    private final Observer mObserver;
    private final ServiceResolveQueue mResolveQueue;
    private final String[] mServiceType;
    private final Map<String, VendorInfo> mVendorInfoHashMap;
    private List<NsdManager.DiscoveryListener> mListeners = new ArrayList();
    public HashMap<String, PrinterHashMap> mVendorHashMap = new HashMap<>();

    public interface Observer {
        void dataSetChanged();

        boolean matchesCriteria(String str, NsdServiceInfo nsdServiceInfo);
    }

    public ServiceListener(Context context, Observer observer, String[] strArr) {
        this.mObserver = observer;
        this.mServiceType = strArr;
        this.mNSDManager = (NsdManager) context.getSystemService("servicediscovery");
        this.mResolveQueue = ServiceResolveQueue.getInstance(this.mNSDManager);
        HashMap map = new HashMap();
        TypedArray typedArrayObtainTypedArray = context.getResources().obtainTypedArray(R.array.known_print_plugin_vendors);
        for (int i = 0; i < typedArrayObtainTypedArray.length(); i++) {
            int resourceId = typedArrayObtainTypedArray.getResourceId(i, 0);
            if (resourceId != 0) {
                VendorInfo vendorInfo = new VendorInfo(context.getResources(), resourceId);
                map.put(vendorInfo.mVendorID, vendorInfo);
                map.put(vendorInfo.mPackageName, vendorInfo);
            }
        }
        typedArrayObtainTypedArray.recycle();
        this.mVendorInfoHashMap = map;
    }

    @Override
    public void serviceResolved(NsdServiceInfo nsdServiceInfo) {
        printerFound(nsdServiceInfo);
    }

    private synchronized void printerFound(NsdServiceInfo nsdServiceInfo) {
        Map.Entry<String, VendorInfo> next;
        if (nsdServiceInfo == null) {
            return;
        }
        if (TextUtils.isEmpty(PrinterHashMap.getKey(nsdServiceInfo))) {
            return;
        }
        String vendor = MDnsUtils.getVendor(nsdServiceInfo);
        if (vendor == null) {
            vendor = "";
        }
        Iterator<Map.Entry<String, VendorInfo>> it = this.mVendorInfoHashMap.entrySet().iterator();
        do {
            if (!it.hasNext()) {
                break;
            }
            next = it.next();
            String[] strArr = next.getValue().mDNSValues;
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (!vendor.equalsIgnoreCase(strArr[i])) {
                    i++;
                } else {
                    vendor = next.getValue().mVendorID;
                    break;
                }
            }
            if (vendor != next.getValue().mVendorID && MDnsUtils.isVendorPrinter(nsdServiceInfo, next.getValue().mDNSValues)) {
                vendor = next.getValue().mVendorID;
            }
        } while (vendor != next.getValue().mVendorID);
        if (TextUtils.isEmpty(vendor)) {
            return;
        }
        if (this.mObserver.matchesCriteria(vendor, nsdServiceInfo)) {
            PrinterHashMap printerHashMap = this.mVendorHashMap.get(vendor);
            if (printerHashMap == null) {
                printerHashMap = new PrinterHashMap();
            }
            boolean z = printerHashMap.addPrinter(nsdServiceInfo) == null;
            this.mVendorHashMap.put(vendor, printerHashMap);
            if (z) {
                this.mObserver.dataSetChanged();
            }
        }
    }

    private synchronized void printerRemoved(NsdServiceInfo nsdServiceInfo) {
        boolean z = false;
        for (String str : this.mVendorHashMap.keySet()) {
            PrinterHashMap printerHashMap = this.mVendorHashMap.get(str);
            boolean z2 = true;
            z |= printerHashMap.removePrinter(nsdServiceInfo) != null;
            if (printerHashMap.isEmpty()) {
                if (this.mVendorHashMap.remove(str) == null) {
                    z2 = false;
                }
                z |= z2;
            }
        }
        if (z) {
            this.mObserver.dataSetChanged();
        }
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
                    ServiceListener.this.mResolveQueue.queueRequest(nsdServiceInfo, ServiceListener.this);
                }

                @Override
                public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                    ServiceListener.this.mResolveQueue.removeRequest(nsdServiceInfo, ServiceListener.this);
                    ServiceListener.this.printerRemoved(nsdServiceInfo);
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
        this.mVendorHashMap.clear();
        this.mListeners.clear();
    }

    public ArrayList<InetAddress> getPrinters() {
        ArrayList<InetAddress> arrayList = new ArrayList<>();
        Iterator<PrinterHashMap> it = this.mVendorHashMap.values().iterator();
        while (it.hasNext()) {
            Iterator<NsdServiceInfo> it2 = it.next().values().iterator();
            while (it2.hasNext()) {
                arrayList.add(it2.next().getHost());
            }
        }
        return arrayList;
    }
}
