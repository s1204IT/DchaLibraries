package com.android.bips.discovery;

import android.net.Uri;
import android.text.TextUtils;
import com.android.bips.BuiltInPrintService;
import com.android.bips.ipp.CapabilitiesCache;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.util.WifiMonitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class ManualDiscovery extends SavedDiscovery {
    private List<CapabilitiesFinder> mAddRequests;
    private CapabilitiesCache mCapabilitiesCache;
    private WifiMonitor mWifiMonitor;
    private static final String TAG = ManualDiscovery.class.getSimpleName();
    private static final Uri[] IPP_URIS = {Uri.parse("ipp://host:631/ipp/print"), Uri.parse("ipp://host:80/ipp/print"), Uri.parse("ipp://host:631/ipp/printer"), Uri.parse("ipp://host:631/ipp"), Uri.parse("ipp://host:631/"), Uri.parse("ipps://host:631/ipp/print"), Uri.parse("ipps://host:443/ipp/print"), Uri.parse("ipps://host:10443/ipp/print")};

    public interface PrinterAddCallback {
        void onFound(DiscoveredPrinter discoveredPrinter, boolean z);

        void onNotFound();
    }

    public ManualDiscovery(BuiltInPrintService builtInPrintService) {
        super(builtInPrintService);
        this.mAddRequests = new ArrayList();
        this.mCapabilitiesCache = getPrintService().getCapabilitiesCache();
    }

    @Override
    void onStart() {
        this.mWifiMonitor = new WifiMonitor(getPrintService(), new WifiMonitor.Listener() {
            @Override
            public final void onConnectionStateChanged(boolean z) {
                ManualDiscovery.lambda$onStart$1(this.f$0, z);
            }
        });
    }

    public static void lambda$onStart$1(final ManualDiscovery manualDiscovery, boolean z) {
        if (z) {
            for (final DiscoveredPrinter discoveredPrinter : manualDiscovery.getSavedPrinters()) {
                manualDiscovery.mCapabilitiesCache.request(discoveredPrinter, false, new CapabilitiesCache.OnLocalPrinterCapabilities() {
                    @Override
                    public final void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities) {
                        ManualDiscovery.lambda$onStart$0(this.f$0, discoveredPrinter, localPrinterCapabilities);
                    }
                });
            }
            return;
        }
        manualDiscovery.allPrintersLost();
    }

    public static void lambda$onStart$0(ManualDiscovery manualDiscovery, DiscoveredPrinter discoveredPrinter, LocalPrinterCapabilities localPrinterCapabilities) {
        if (localPrinterCapabilities != null) {
            manualDiscovery.printerFound(discoveredPrinter);
        }
    }

    @Override
    void onStop() {
        this.mWifiMonitor.close();
        allPrintersLost();
    }

    public void addManualPrinter(Uri uri, PrinterAddCallback printerAddCallback) {
        int port = uri.getPort();
        String path = uri.getPath();
        String host = uri.getHost();
        String scheme = uri.getScheme();
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (Uri uri2 : IPP_URIS) {
            String scheme2 = uri2.getScheme();
            if (TextUtils.isEmpty(scheme) || scheme2.equals(scheme)) {
                StringBuilder sb = new StringBuilder();
                sb.append(host);
                sb.append(":");
                sb.append(port == -1 ? uri2.getPort() : port);
                linkedHashSet.add(uri2.buildUpon().scheme(scheme2).encodedAuthority(sb.toString()).path(TextUtils.isEmpty(path) ? uri2.getPath() : path).build());
            }
        }
        this.mAddRequests.add(new CapabilitiesFinder(linkedHashSet, printerAddCallback));
    }

    public void cancelAddManualPrinter(PrinterAddCallback printerAddCallback) {
        for (CapabilitiesFinder capabilitiesFinder : this.mAddRequests) {
            if (capabilitiesFinder.mFinalCallback == printerAddCallback) {
                this.mAddRequests.remove(capabilitiesFinder);
                capabilitiesFinder.cancel();
                return;
            }
        }
    }

    private class CapabilitiesFinder {
        private final PrinterAddCallback mFinalCallback;
        private final List<CapabilitiesCache.OnLocalPrinterCapabilities> mRequests = new ArrayList();

        CapabilitiesFinder(Collection<Uri> collection, PrinterAddCallback printerAddCallback) {
            this.mFinalCallback = printerAddCallback;
            for (final Uri uri : collection) {
                CapabilitiesCache.OnLocalPrinterCapabilities onLocalPrinterCapabilities = new CapabilitiesCache.OnLocalPrinterCapabilities() {
                    @Override
                    public void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities) {
                        CapabilitiesFinder.this.mRequests.remove(this);
                        CapabilitiesFinder.this.handleCapabilities(uri, localPrinterCapabilities);
                    }
                };
                this.mRequests.add(onLocalPrinterCapabilities);
                ManualDiscovery.this.mCapabilitiesCache.remove(uri);
                ManualDiscovery.this.mCapabilitiesCache.request(new DiscoveredPrinter(null, "", uri, null), true, onLocalPrinterCapabilities);
            }
        }

        void handleCapabilities(Uri uri, LocalPrinterCapabilities localPrinterCapabilities) {
            if (localPrinterCapabilities == null) {
                if (this.mRequests.isEmpty()) {
                    ManualDiscovery.this.mAddRequests.remove(this);
                    this.mFinalCallback.onNotFound();
                    return;
                }
                return;
            }
            Iterator<CapabilitiesCache.OnLocalPrinterCapabilities> it = this.mRequests.iterator();
            while (it.hasNext()) {
                ManualDiscovery.this.mCapabilitiesCache.cancel(it.next());
            }
            this.mRequests.clear();
            DiscoveredPrinter discoveredPrinter = new DiscoveredPrinter(TextUtils.isEmpty(localPrinterCapabilities.uuid) ? null : Uri.parse(localPrinterCapabilities.uuid), TextUtils.isEmpty(localPrinterCapabilities.name) ? uri.getHost() : localPrinterCapabilities.name, uri, localPrinterCapabilities.location);
            if (localPrinterCapabilities.isSupported && ManualDiscovery.this.addSavedPrinter(discoveredPrinter)) {
                ManualDiscovery.this.printerFound(discoveredPrinter);
            }
            ManualDiscovery.this.mAddRequests.remove(this);
            this.mFinalCallback.onFound(discoveredPrinter, localPrinterCapabilities.isSupported);
        }

        public void cancel() {
            Iterator<CapabilitiesCache.OnLocalPrinterCapabilities> it = this.mRequests.iterator();
            while (it.hasNext()) {
                ManualDiscovery.this.mCapabilitiesCache.cancel(it.next());
            }
            this.mRequests.clear();
        }
    }
}
