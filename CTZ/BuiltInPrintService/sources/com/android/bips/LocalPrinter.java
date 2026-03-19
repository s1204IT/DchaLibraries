package com.android.bips;

import android.net.Uri;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.widget.Toast;
import com.android.bips.discovery.ConnectionListener;
import com.android.bips.discovery.DiscoveredPrinter;
import com.android.bips.ipp.CapabilitiesCache;
import com.android.bips.jni.LocalPrinterCapabilities;
import com.android.bips.p2p.P2pPrinterConnection;
import com.android.bips.p2p.P2pUtils;
import java.net.InetAddress;
import java.util.Collections;

class LocalPrinter implements CapabilitiesCache.OnLocalPrinterCapabilities {
    private static final String TAG = LocalPrinter.class.getSimpleName();
    private LocalPrinterCapabilities mCapabilities;
    private DiscoveredPrinter mDiscoveredPrinter;
    private final BuiltInPrintService mPrintService;
    private final PrinterId mPrinterId;
    private final LocalDiscoverySession mSession;
    private P2pPrinterConnection mTrackingConnection;
    private long mLastSeenTime = System.currentTimeMillis();
    private boolean mFound = true;
    private boolean mTracking = false;

    LocalPrinter(BuiltInPrintService builtInPrintService, LocalDiscoverySession localDiscoverySession, DiscoveredPrinter discoveredPrinter) {
        this.mPrintService = builtInPrintService;
        this.mSession = localDiscoverySession;
        this.mDiscoveredPrinter = discoveredPrinter;
        this.mPrinterId = discoveredPrinter.getId(builtInPrintService);
    }

    public InetAddress getAddress() {
        if (this.mCapabilities != null) {
            return this.mCapabilities.inetAddress;
        }
        return null;
    }

    boolean isExpired() {
        return !this.mFound && System.currentTimeMillis() - this.mLastSeenTime > 3000;
    }

    LocalPrinterCapabilities getCapabilities() {
        return this.mCapabilities;
    }

    PrinterInfo createPrinterInfo(boolean z) {
        boolean z2;
        if (this.mCapabilities == null) {
            if (P2pUtils.isP2p(this.mDiscoveredPrinter)) {
                return new PrinterInfo.Builder(this.mPrinterId, this.mDiscoveredPrinter.name, 1).setIconResourceId(R.drawable.ic_printer).setDescription(this.mPrintService.getDescription(this.mDiscoveredPrinter)).build();
            }
            if (!z) {
                return null;
            }
        } else if (!this.mCapabilities.isSupported) {
            return null;
        }
        DiscoveredPrinter printer = this.mPrintService.getDiscovery().getPrinter(this.mDiscoveredPrinter.getUri());
        if (printer == null) {
            return null;
        }
        if (!this.mFound || this.mCapabilities == null) {
            z2 = false;
        } else {
            z2 = true;
        }
        PrinterInfo.Builder description = new PrinterInfo.Builder(this.mPrinterId, printer.name, z2 ? 1 : 3).setIconResourceId(R.drawable.ic_printer).setDescription(this.mPrintService.getDescription(this.mDiscoveredPrinter));
        if (this.mCapabilities != null) {
            PrinterCapabilitiesInfo.Builder builder = new PrinterCapabilitiesInfo.Builder(this.mPrinterId);
            this.mCapabilities.buildCapabilities(this.mPrintService, builder);
            description.setCapabilities(builder.build());
        }
        return description.build();
    }

    @Override
    public void onCapabilities(LocalPrinterCapabilities localPrinterCapabilities) {
        if (this.mSession.isDestroyed() || !this.mSession.isKnown(this.mPrinterId)) {
            return;
        }
        if (localPrinterCapabilities == null) {
            this.mSession.removePrinters(Collections.singletonList(this.mPrinterId));
        } else {
            this.mCapabilities = localPrinterCapabilities;
            this.mSession.handlePrinter(this);
        }
    }

    PrinterId getPrinterId() {
        return this.mPrinterId;
    }

    boolean isFound() {
        return this.mFound;
    }

    void found(DiscoveredPrinter discoveredPrinter) {
        this.mDiscoveredPrinter = discoveredPrinter;
        this.mLastSeenTime = System.currentTimeMillis();
        this.mFound = true;
        LocalPrinterCapabilities localPrinterCapabilities = this.mPrintService.getCapabilitiesCache().get(this.mDiscoveredPrinter);
        if (localPrinterCapabilities != null) {
            onCapabilities(localPrinterCapabilities);
            return;
        }
        this.mSession.handlePrinter(this);
        if (!P2pUtils.isP2p(this.mDiscoveredPrinter)) {
            this.mPrintService.getCapabilitiesCache().request(this.mDiscoveredPrinter, this.mSession.isPriority(this.mPrinterId), this);
        } else if (this.mTracking) {
            startTracking();
        }
    }

    public void track() {
        startTracking();
    }

    private void startTracking() {
        this.mTracking = true;
        if (this.mTrackingConnection != null) {
            return;
        }
        if (P2pUtils.isP2p(this.mDiscoveredPrinter) || P2pUtils.isOnConnectedInterface(this.mPrintService, this.mDiscoveredPrinter)) {
            this.mTrackingConnection = new P2pPrinterConnection(this.mPrintService, this.mDiscoveredPrinter, new ConnectionListener() {
                @Override
                public void onConnectionComplete(DiscoveredPrinter discoveredPrinter) {
                    if (discoveredPrinter == null) {
                        LocalPrinter.this.mTrackingConnection = null;
                    }
                }

                @Override
                public void onConnectionDelayed(boolean z) {
                    if (z) {
                        Toast.makeText(LocalPrinter.this.mPrintService, R.string.connect_hint_text, 1).show();
                    }
                }
            });
        }
    }

    void stopTracking() {
        if (this.mTrackingConnection != null) {
            this.mTrackingConnection.close();
            this.mTrackingConnection = null;
        }
        this.mTracking = false;
    }

    void notFound() {
        this.mFound = false;
        this.mLastSeenTime = System.currentTimeMillis();
    }

    public Uri getUuid() {
        return this.mDiscoveredPrinter.uuid;
    }

    public String toString() {
        return this.mDiscoveredPrinter.toString();
    }
}
