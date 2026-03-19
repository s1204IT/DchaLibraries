package com.android.bips.discovery;

import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.bips.BuiltInPrintService;
import com.android.bips.discovery.MdnsDiscovery;
import com.android.bips.discovery.MdnsDiscovery.Resolver;
import com.android.bips.discovery.NsdResolveQueue;
import com.android.bips.jni.BackendConstants;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MdnsDiscovery extends Discovery {
    private static final String TAG = MdnsDiscovery.class.getSimpleName();
    private WifiManager.MulticastLock mMulticastLock;
    private final NsdResolveQueue mNsdResolveQueue;
    private final List<Resolver> mResolvers;
    private final List<NsdServiceListener> mServiceListeners;
    private final String mServiceName;

    public MdnsDiscovery(BuiltInPrintService builtInPrintService, String str) {
        byte b;
        super(builtInPrintService);
        this.mServiceListeners = new ArrayList();
        this.mResolvers = new ArrayList();
        int iHashCode = str.hashCode();
        if (iHashCode != 104489) {
            b = (iHashCode == 3239274 && str.equals("ipps")) ? (byte) 1 : (byte) -1;
        } else if (str.equals("ipp")) {
            b = 0;
        }
        switch (b) {
            case BackendConstants.STATUS_OK:
                this.mServiceName = "_ipp._tcp";
                break;
            case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                this.mServiceName = "_ipps._tcp";
                break;
            default:
                throw new IllegalArgumentException("unrecognized scheme " + str);
        }
        this.mNsdResolveQueue = builtInPrintService.getNsdResolveQueue();
    }

    private static DiscoveredPrinter toNetworkPrinter(NsdServiceInfo nsdServiceInfo) {
        Uri uri;
        if ("F".equals(getStringAttribute(nsdServiceInfo, "print_wfds"))) {
            return null;
        }
        String stringAttribute = getStringAttribute(nsdServiceInfo, "rp");
        if (TextUtils.isEmpty(stringAttribute)) {
            return null;
        }
        if (stringAttribute.startsWith("/")) {
            stringAttribute = stringAttribute.substring(1);
        }
        String stringAttribute2 = getStringAttribute(nsdServiceInfo, "UUID");
        if (!TextUtils.isEmpty(stringAttribute2)) {
            uri = Uri.parse("urn:uuid:" + stringAttribute2);
        } else {
            uri = null;
        }
        if (!(nsdServiceInfo.getHost() instanceof Inet4Address)) {
            return null;
        }
        return new DiscoveredPrinter(uri, nsdServiceInfo.getServiceName(), Uri.parse((nsdServiceInfo.getServiceType().contains("_ipps._tcp") ? "ipps" : "ipp") + "://" + nsdServiceInfo.getHost().getHostAddress() + ":" + nsdServiceInfo.getPort() + "/" + stringAttribute), getStringAttribute(nsdServiceInfo, "note"));
    }

    private static String getStringAttribute(NsdServiceInfo nsdServiceInfo, String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        for (Map.Entry<String, byte[]> entry : nsdServiceInfo.getAttributes().entrySet()) {
            if (entry.getKey().toLowerCase(Locale.US).equals(lowerCase) && entry.getValue() != null) {
                return new String(entry.getValue());
            }
        }
        return null;
    }

    @Override
    void onStart() {
        NsdServiceListener nsdServiceListener = new NsdServiceListener() {
            @Override
            public void onStartDiscoveryFailed(String str, int i) {
            }
        };
        WifiManager wifiManager = (WifiManager) getPrintService().getSystemService(WifiManager.class);
        if (wifiManager != null) {
            if (this.mMulticastLock == null) {
                this.mMulticastLock = wifiManager.createMulticastLock(getClass().getName());
            }
            this.mMulticastLock.acquire();
        }
        this.mNsdResolveQueue.getNsdManager().discoverServices(this.mServiceName, 1, nsdServiceListener);
        this.mServiceListeners.add(nsdServiceListener);
    }

    @Override
    void onStop() {
        NsdManager nsdManager = this.mNsdResolveQueue.getNsdManager();
        Iterator<NsdServiceListener> it = this.mServiceListeners.iterator();
        while (it.hasNext()) {
            nsdManager.stopServiceDiscovery(it.next());
        }
        this.mServiceListeners.clear();
        Iterator<Resolver> it2 = this.mResolvers.iterator();
        while (it2.hasNext()) {
            it2.next().cancel();
        }
        this.mResolvers.clear();
        if (this.mMulticastLock != null) {
            this.mMulticastLock.release();
        }
    }

    private abstract class NsdServiceListener implements NsdManager.DiscoveryListener {
        private NsdServiceListener() {
        }

        @Override
        public void onStopDiscoveryFailed(String str, int i) {
            Log.w(MdnsDiscovery.TAG, "onStopDiscoveryFailed: " + i);
        }

        @Override
        public void onDiscoveryStarted(String str) {
        }

        @Override
        public void onDiscoveryStopped(String str) {
            Handler handler = MdnsDiscovery.this.getHandler();
            final MdnsDiscovery mdnsDiscovery = MdnsDiscovery.this;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    mdnsDiscovery.allPrintersLost();
                }
            });
        }

        @Override
        public void onServiceFound(final NsdServiceInfo nsdServiceInfo) {
            MdnsDiscovery.this.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    MdnsDiscovery.NsdServiceListener nsdServiceListener = this.f$0;
                    MdnsDiscovery.this.mResolvers.add(MdnsDiscovery.this.new Resolver(nsdServiceInfo));
                }
            });
        }

        @Override
        public void onServiceLost(final NsdServiceInfo nsdServiceInfo) {
            MdnsDiscovery.this.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    MdnsDiscovery.NsdServiceListener.lambda$onServiceLost$1(this.f$0, nsdServiceInfo);
                }
            });
        }

        public static void lambda$onServiceLost$1(NsdServiceListener nsdServiceListener, NsdServiceInfo nsdServiceInfo) {
            for (DiscoveredPrinter discoveredPrinter : MdnsDiscovery.this.getPrinters()) {
                if (TextUtils.equals(discoveredPrinter.name, nsdServiceInfo.getServiceName())) {
                    MdnsDiscovery.this.printerLost(discoveredPrinter.getUri());
                    return;
                }
            }
        }
    }

    private class Resolver implements NsdManager.ResolveListener {
        private final NsdResolveQueue.NsdResolveRequest mResolveAttempt;

        Resolver(NsdServiceInfo nsdServiceInfo) {
            this.mResolveAttempt = MdnsDiscovery.this.mNsdResolveQueue.resolve(nsdServiceInfo, this);
        }

        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
            MdnsDiscovery.this.mResolvers.remove(this);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
            DiscoveredPrinter networkPrinter;
            MdnsDiscovery.this.mResolvers.remove(this);
            if (!MdnsDiscovery.this.isStarted() || (networkPrinter = MdnsDiscovery.toNetworkPrinter(nsdServiceInfo)) == null) {
                return;
            }
            MdnsDiscovery.this.printerFound(networkPrinter);
        }

        void cancel() {
            this.mResolveAttempt.cancel();
        }
    }
}
