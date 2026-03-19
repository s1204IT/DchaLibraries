package com.android.printservice.recommendation.plugin.samsung;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import com.android.printservice.recommendation.PrintServicePlugin;
import com.android.printservice.recommendation.R;
import com.android.printservice.recommendation.util.MDNSFilteredDiscovery;
import java.util.HashSet;
import java.util.Set;

public class SamsungRecommendationPlugin implements PrintServicePlugin {
    private static final Set<String> ALL_MDNS_SERVICES = new HashSet<String>() {
        {
            addAll(PrinterFilterMopria.MOPRIA_MDNS_SERVICES);
            addAll(PrinterFilterSamsung.SAMSUNG_MDNS_SERVICES);
        }
    };
    private final Context mContext;
    private final MDNSFilteredDiscovery mMDNSFilteredDiscovery;
    private final PrinterFilterSamsung mPrinterFilterSamsung = new PrinterFilterSamsung();
    private final PrinterFilterMopria mPrinterFilterMopria = new PrinterFilterMopria();

    public SamsungRecommendationPlugin(Context context) {
        this.mContext = context;
        this.mMDNSFilteredDiscovery = new MDNSFilteredDiscovery(context, ALL_MDNS_SERVICES, new MDNSFilteredDiscovery.PrinterFilter() {
            @Override
            public final boolean matchesCriteria(NsdServiceInfo nsdServiceInfo) {
                return SamsungRecommendationPlugin.lambda$new$0(this.f$0, nsdServiceInfo);
            }
        });
    }

    public static boolean lambda$new$0(SamsungRecommendationPlugin samsungRecommendationPlugin, NsdServiceInfo nsdServiceInfo) {
        return samsungRecommendationPlugin.mPrinterFilterSamsung.matchesCriteria(nsdServiceInfo) || samsungRecommendationPlugin.mPrinterFilterMopria.matchesCriteria(nsdServiceInfo);
    }

    @Override
    public int getName() {
        return R.string.plugin_vendor_samsung;
    }

    @Override
    public CharSequence getPackageName() {
        return this.mContext.getString(R.string.plugin_package_samsung);
    }

    @Override
    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception {
        this.mMDNSFilteredDiscovery.start(printerDiscoveryCallback);
    }

    @Override
    public void stop() throws Exception {
        this.mMDNSFilteredDiscovery.stop();
    }
}
