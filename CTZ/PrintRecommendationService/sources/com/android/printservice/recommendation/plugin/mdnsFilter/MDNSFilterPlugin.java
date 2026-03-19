package com.android.printservice.recommendation.plugin.mdnsFilter;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import com.android.printservice.recommendation.PrintServicePlugin;
import com.android.printservice.recommendation.util.MDNSFilteredDiscovery;
import com.android.printservice.recommendation.util.MDNSUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MDNSFilterPlugin implements PrintServicePlugin {
    private static final Set<String> PRINTER_SERVICE_TYPES = new HashSet<String>() {
        {
            add("_ipp._tcp");
        }
    };
    private final MDNSFilteredDiscovery mMDNSFilteredDiscovery;
    private final int mName;
    private final CharSequence mPackageName;

    private static class VendorNameFilter implements MDNSFilteredDiscovery.PrinterFilter {
        private final Set<String> mMDNSNames;

        VendorNameFilter(Set<String> set) {
            this.mMDNSNames = new HashSet(set);
        }

        @Override
        public boolean matchesCriteria(NsdServiceInfo nsdServiceInfo) {
            return MDNSUtils.isVendorPrinter(nsdServiceInfo, this.mMDNSNames);
        }
    }

    public MDNSFilterPlugin(Context context, String str, CharSequence charSequence, List<String> list) {
        this.mName = context.getResources().getIdentifier(str, null, "com.android.printservice.recommendation");
        this.mPackageName = charSequence;
        this.mMDNSFilteredDiscovery = new MDNSFilteredDiscovery(context, PRINTER_SERVICE_TYPES, new VendorNameFilter(new HashSet(list)));
    }

    @Override
    public CharSequence getPackageName() {
        return this.mPackageName;
    }

    @Override
    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception {
        this.mMDNSFilteredDiscovery.start(printerDiscoveryCallback);
    }

    @Override
    public int getName() {
        return this.mName;
    }

    @Override
    public void stop() throws Exception {
        this.mMDNSFilteredDiscovery.stop();
    }
}
