package com.android.bips.discovery;

import android.net.Uri;
import com.android.bips.discovery.Discovery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MultiDiscovery extends Discovery {
    private static final String TAG = MultiDiscovery.class.getSimpleName();
    private final Discovery.Listener mChildListener;
    private final List<Discovery> mDiscoveries;
    private final List<Discovery> mStartedDiscoveries;

    public MultiDiscovery(Discovery discovery, Discovery... discoveryArr) {
        super(discovery.getPrintService());
        this.mDiscoveries = new ArrayList();
        this.mStartedDiscoveries = new ArrayList();
        this.mDiscoveries.add(discovery);
        this.mDiscoveries.addAll(Arrays.asList(discoveryArr));
        this.mChildListener = new Discovery.Listener() {
            @Override
            public void onPrinterFound(DiscoveredPrinter discoveredPrinter) {
                MultiDiscovery.this.printerFound(MultiDiscovery.this.first(discoveredPrinter.getUri()));
            }

            @Override
            public void onPrinterLost(DiscoveredPrinter discoveredPrinter) {
                DiscoveredPrinter discoveredPrinterFirst = MultiDiscovery.this.first(discoveredPrinter.getUri());
                if (discoveredPrinterFirst == null) {
                    MultiDiscovery.this.printerLost(discoveredPrinter.getUri());
                } else {
                    MultiDiscovery.this.printerFound(discoveredPrinterFirst);
                }
            }
        };
    }

    private DiscoveredPrinter first(Uri uri) {
        Iterator<Discovery> it = getChildren().iterator();
        while (it.hasNext()) {
            DiscoveredPrinter printer = it.next().getPrinter(uri);
            if (printer != null) {
                return printer;
            }
        }
        return null;
    }

    @Override
    void onStart() {
        for (Discovery discovery : this.mDiscoveries) {
            discovery.start(this.mChildListener);
            this.mStartedDiscoveries.add(discovery);
        }
    }

    private void stopAndClearAll() {
        Iterator<Discovery> it = this.mStartedDiscoveries.iterator();
        while (it.hasNext()) {
            it.next().stop(this.mChildListener);
        }
        this.mStartedDiscoveries.clear();
        allPrintersLost();
    }

    @Override
    void onStop() {
        stopAndClearAll();
    }

    @Override
    Collection<Discovery> getChildren() {
        ArrayList arrayList = new ArrayList();
        Iterator<Discovery> it = this.mDiscoveries.iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().getChildren());
        }
        return arrayList;
    }
}
