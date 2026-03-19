package com.android.printservice.recommendation.util;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import androidx.core.util.Preconditions;
import com.android.printservice.recommendation.PrintServicePlugin;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MDNSFilteredDiscovery implements NsdManager.DiscoveryListener {
    private PrintServicePlugin.PrinterDiscoveryCallback mCallback;
    private final Context mContext;
    private final PrinterFilter mPrinterFilter;
    private final HashSet<String> mServiceTypes;
    private final NsdResolveQueue mResolveQueue = NsdResolveQueue.getInstance();
    private final HashSet<InetAddress> mPrinters = new HashSet<>();

    public interface PrinterFilter {
        boolean matchesCriteria(NsdServiceInfo nsdServiceInfo);
    }

    public MDNSFilteredDiscovery(Context context, Set<String> set, PrinterFilter printerFilter) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "context");
        this.mServiceTypes = new HashSet<>(Preconditions.checkCollectionNotEmpty(Preconditions.checkCollectionElementsNotNull(set, "serviceTypes"), "serviceTypes"));
        this.mPrinterFilter = (PrinterFilter) Preconditions.checkNotNull(printerFilter, "printerFilter");
    }

    private NsdManager getNDSManager() {
        return (NsdManager) this.mContext.getSystemService("servicediscovery");
    }

    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) {
        this.mCallback = printerDiscoveryCallback;
        this.mCallback.onChanged(new ArrayList(this.mPrinters));
        Iterator<String> it = this.mServiceTypes.iterator();
        while (it.hasNext()) {
            DiscoveryListenerMultiplexer.addListener(getNDSManager(), it.next(), this);
        }
    }

    public void stop() {
        this.mCallback.onChanged(null);
        this.mCallback = null;
        for (int i = 0; i < this.mServiceTypes.size(); i++) {
            DiscoveryListenerMultiplexer.removeListener(getNDSManager(), this);
        }
    }

    @Override
    public void onStartDiscoveryFailed(String str, int i) {
        Log.w("MDNSFilteredDiscovery", "Failed to start network discovery for type " + str + ": " + i);
    }

    @Override
    public void onStopDiscoveryFailed(String str, int i) {
        Log.w("MDNSFilteredDiscovery", "Failed to stop network discovery for type " + str + ": " + i);
    }

    @Override
    public void onDiscoveryStarted(String str) {
    }

    @Override
    public void onDiscoveryStopped(String str) {
        this.mPrinters.clear();
    }

    @Override
    public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
        this.mResolveQueue.resolve(getNDSManager(), nsdServiceInfo, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo2, int i) {
                Log.w("MDNSFilteredDiscovery", "Service found: could not resolve " + nsdServiceInfo2 + ": " + i);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo2) {
                if (MDNSFilteredDiscovery.this.mPrinterFilter.matchesCriteria(nsdServiceInfo2) && MDNSFilteredDiscovery.this.mCallback != null && MDNSFilteredDiscovery.this.mPrinters.add(nsdServiceInfo2.getHost())) {
                    MDNSFilteredDiscovery.this.mCallback.onChanged(new ArrayList(MDNSFilteredDiscovery.this.mPrinters));
                }
            }
        });
    }

    @Override
    public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
        this.mResolveQueue.resolve(getNDSManager(), nsdServiceInfo, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo2, int i) {
                Log.w("MDNSFilteredDiscovery", "Service lost: Could not resolve " + nsdServiceInfo2 + ": " + i);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo2) {
                if (MDNSFilteredDiscovery.this.mPrinterFilter.matchesCriteria(nsdServiceInfo2) && MDNSFilteredDiscovery.this.mCallback != null && MDNSFilteredDiscovery.this.mPrinters.remove(nsdServiceInfo2.getHost())) {
                    MDNSFilteredDiscovery.this.mCallback.onChanged(new ArrayList(MDNSFilteredDiscovery.this.mPrinters));
                }
            }
        });
    }
}
