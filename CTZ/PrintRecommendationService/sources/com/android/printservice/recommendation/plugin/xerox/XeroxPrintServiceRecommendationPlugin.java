package com.android.printservice.recommendation.plugin.xerox;

import android.content.Context;
import android.net.nsd.NsdManager;
import com.android.printservice.recommendation.PrintServicePlugin;
import com.android.printservice.recommendation.R;
import com.android.printservice.recommendation.plugin.xerox.ServiceResolver;

public class XeroxPrintServiceRecommendationPlugin implements PrintServicePlugin, ServiceResolver.Observer {
    protected final NsdManager mNSDManager;
    protected final ServiceResolver mServiceResolver;
    protected final VendorInfo mVendorInfo;
    protected final Object mLock = new Object();
    protected PrintServicePlugin.PrinterDiscoveryCallback mDiscoveryCallback = null;
    private final int mVendorStringID = R.string.plugin_vendor_xerox;
    private final String PDL__PDF = "application/pdf";
    private final String[] mServices = {"_ipp._tcp"};

    public XeroxPrintServiceRecommendationPlugin(Context context) {
        this.mNSDManager = (NsdManager) context.getSystemService("servicediscovery");
        this.mVendorInfo = new VendorInfo(context.getResources(), R.array.known_print_vendor_info_for_xerox);
        this.mServiceResolver = new ServiceResolver(context, this, this.mVendorInfo, this.mServices, new String[]{"application/pdf"});
    }

    @Override
    public int getName() {
        return R.string.plugin_vendor_xerox;
    }

    @Override
    public CharSequence getPackageName() {
        return this.mVendorInfo.mPackageName;
    }

    @Override
    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception {
        synchronized (this.mLock) {
            this.mDiscoveryCallback = printerDiscoveryCallback;
            this.mServiceResolver.start();
        }
    }

    @Override
    public void stop() throws Exception {
        synchronized (this.mLock) {
            this.mDiscoveryCallback = null;
            this.mServiceResolver.stop();
        }
    }

    @Override
    public void dataSetChanged() {
        synchronized (this.mLock) {
            if (this.mDiscoveryCallback != null) {
                this.mDiscoveryCallback.onChanged(this.mServiceResolver.getPrinters());
            }
        }
    }
}
