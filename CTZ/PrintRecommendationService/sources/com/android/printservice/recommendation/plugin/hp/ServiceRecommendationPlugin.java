package com.android.printservice.recommendation.plugin.hp;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import com.android.printservice.recommendation.PrintServicePlugin;
import com.android.printservice.recommendation.plugin.hp.ServiceListener;
import java.net.InetAddress;
import java.util.ArrayList;

public abstract class ServiceRecommendationPlugin implements PrintServicePlugin, ServiceListener.Observer {
    protected final ServiceListener mListener;
    protected final NsdManager mNSDManager;
    protected final VendorInfo mVendorInfo;
    private final int mVendorStringID;
    protected final Object mLock = new Object();
    protected PrintServicePlugin.PrinterDiscoveryCallback mCallback = null;

    protected ServiceRecommendationPlugin(Context context, int i, VendorInfo vendorInfo, String[] strArr) {
        this.mNSDManager = (NsdManager) context.getSystemService("servicediscovery");
        this.mVendorStringID = i;
        this.mVendorInfo = vendorInfo;
        this.mListener = new ServiceListener(context, this, strArr);
    }

    @Override
    public int getName() {
        return this.mVendorStringID;
    }

    @Override
    public CharSequence getPackageName() {
        return this.mVendorInfo.mPackageName;
    }

    @Override
    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception {
        synchronized (this.mLock) {
            this.mCallback = printerDiscoveryCallback;
        }
        this.mListener.start();
    }

    @Override
    public void stop() throws Exception {
        synchronized (this.mLock) {
            this.mCallback = null;
        }
        this.mListener.stop();
    }

    @Override
    public void dataSetChanged() {
        synchronized (this.mLock) {
            if (this.mCallback != null) {
                this.mCallback.onChanged(getPrinters());
            }
        }
    }

    public boolean matchesCriteria(String str, NsdServiceInfo nsdServiceInfo) {
        return TextUtils.equals(str, this.mVendorInfo.mVendorID);
    }

    public ArrayList<InetAddress> getPrinters() {
        return this.mListener.getPrinters();
    }
}
